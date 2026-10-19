## 1. Bức tranh tổng thể của project

Project là một REST API **Spring Boot** quản lý Customer / Order / Product / Category / Voucher,
trọng tâm là **nghiệp vụ kiểm tra voucher có áp dụng được cho đơn hàng hay không**.

```
Browser (index.html)
   │  HTTP JSON
   ▼
Controller      → nhận request, trả response (không chứa nghiệp vụ)
   ▼
Service (impl)  → nghiệp vụ, validate, quy tắc voucher
   ▼
Repository      → Spring Data JPA, nói chuyện với DB
   ▼
Entity ⇄ Bảng trong PostgreSQL
```

Cấu trúc package: `controller`, `service` + `service/impl`, `repository`, `entity`, `request` (DTO nhận vào),
`enums`, `exception`.

**Câu hỏi phỏng vấn hay gặp:** "Tại sao chia layer?"
→ Tách trách nhiệm (Single Responsibility), dễ test từng tầng (mock repository khi test service),
đổi DB / đổi giao diện không ảnh hưởng nghiệp vụ.

---

## 2. Spring Boot cốt lõi

### 2.1 IoC & Dependency Injection
- **IoC (Inversion of Control):** không tự `new` object, để Spring container tạo và quản lý (gọi là *bean*).
- **DI:** Spring "tiêm" bean cần thiết vào class qua **constructor** (cách project đang dùng, nên dùng nhất).
- Vì sao constructor injection tốt hơn `@Autowired` trên field? Field `final` (bất biến), dễ viết unit test
  (tự `new Service(mockRepo)`), phát hiện thiếu dependency ngay lúc khởi động, tránh circular dependency ngầm.

### 2.2 Các annotation chính
| Annotation | Ý nghĩa |
|---|---|
| `@SpringBootApplication` | = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@RestController` | = `@Controller` + `@ResponseBody` (trả JSON) |
| `@Service`, `@Repository`, `@Component` | Đăng ký bean; `@Repository` còn dịch exception của DB sang `DataAccessException` |
| `@RequestMapping/@GetMapping/@PostMapping` | Map URL + HTTP method vào method |
| `@RequestParam`, `@PathVariable`, `@RequestBody` | Lấy dữ liệu từ query string / đường dẫn / body JSON |
| `@RestControllerAdvice` + `@ExceptionHandler` | Bắt exception toàn cục, trả lỗi thống nhất |

### 2.3 Auto-configuration hoạt động thế nào?
Spring Boot quét classpath. Thấy driver `postgresql` + `spring-boot-starter-data-jpa` + có
`spring.datasource.url` → tự tạo `DataSource` (HikariCP), `EntityManagerFactory`, `TransactionManager`.
Không cần viết XML hay config tay. Đây là lý do **đổi từ H2 sang PostgreSQL chỉ cần đổi dependency + properties**.

### 2.4 Xử lý lỗi tập trung
`GlobalExceptionHandler`:
- `NotFoundException` → HTTP **404**
- `IllegalArgumentException`, `IllegalStateException` → HTTP **400**
Body luôn có dạng `{"message": "..."}` để frontend hiển thị.
Lợi ích: controller/service chỉ `throw`, không phải `try/catch` rải rác.

---

## 3. JPA / Hibernate

### 3.1 Khái niệm
- **JPA** là *chuẩn* (interface/annotation), **Hibernate** là *implementation*, **Spring Data JPA** là lớp bọc
  phía trên giúp bạn chỉ cần khai báo `interface XRepository extends JpaRepository<X, Long>`.
- **ORM:** ánh xạ class Java ⇄ bảng DB, object ⇄ row.

### 3.2 Entity trong project
- `@Entity`, `@Table(name = "orders")` — tên bảng `orders` vì `order` là từ khóa SQL (ORDER BY).
- `@Id` + `@GeneratedValue(strategy = IDENTITY)`:
  - Trên PostgreSQL → cột `bigserial/identity`, DB tự tăng.
  - `SEQUENCE` hiệu quả hơn cho batch insert vì Hibernate lấy trước một dải id; `IDENTITY` buộc insert ngay để biết id → **tắt JDBC batch insert**.
- `@Column(nullable = false, unique = true, precision = 19, scale = 2)` — ràng buộc ở cả mức DDL.

### 3.3 `ddl-auto`
| Giá trị | Hành vi | Dùng khi |
|---|---|---|
| `create-drop` | Tạo schema lúc start, xóa lúc tắt | H2 in-memory, demo |
| `create` | Xóa và tạo lại mỗi lần chạy | Dev |
| `update` | Chỉ thêm bảng/cột mới, không xóa | Dev có DB bền (project hiện tại) |
| `validate` | Chỉ kiểm tra entity khớp schema | **Production** |
| `none` | Không làm gì | Production + Flyway/Liquibase |

Production nên dùng **Flyway / Liquibase** (migration có version, review được, rollback được) thay vì để Hibernate tự sửa schema.

### 3.4 Những thứ phải nắm
- **Persistence context / first-level cache:** trong 1 transaction, `findById` hai lần cùng id chỉ query 1 lần.
- **Lazy vs Eager:** `@OneToMany` mặc định LAZY, `@ManyToOne` mặc định EAGER.
- **N+1 problem:** load 100 order rồi mỗi order lazy-load customer → 1 + 100 query.
  Giải pháp: `JOIN FETCH`, `@EntityGraph`, batch size.
- **`save()` vừa insert vừa update:** id null → `persist`; có id → `merge`.
- **Derived query:** `findByCode(String code)` — Spring tự sinh SQL từ tên method.
- **`equals/hashCode` cho entity:** nên dựa trên id/business key, tránh dùng toàn bộ field.

---

## 4. Chuyển H2 → PostgreSQL

### 4.1 H2 vs PostgreSQL
| | H2 (in-memory) | PostgreSQL |
|---|---|---|
| Lưu trữ | RAM, mất khi tắt app | Đĩa, bền vững |
| Mục đích | Demo / test nhanh | Production |
| Tính năng | Giả lập, thiếu nhiều tính năng | Đầy đủ: index nâng cao, JSONB, MVCC, replication |
| Setup | Không cần | Cần server (Docker) |

### 4.2 Những gì đã đổi
1. `pom.xml`: bỏ `h2` + `spring-boot-h2console`, thêm `org.postgresql:postgresql` (scope `runtime`
   vì code không import trực tiếp driver).
2. `application.properties`: `url=jdbc:postgresql://localhost:5432/voucherdb`, username/password đọc từ
   biến môi trường (`${DB_URL:...}`) — **không hard-code mật khẩu production vào code**.
3. `ddl-auto`: `create-drop` → `update` để dữ liệu còn lại sau khi restart.
4. `docker-compose.yml`: chạy PostgreSQL bằng `docker compose up -d`, dữ liệu lưu trong volume `pgdata`.

### 4.3 Vì sao chọn PostgreSQL thay vì MySQL?
- Tuân thủ chuẩn SQL tốt hơn, hỗ trợ transaction DDL, kiểu dữ liệu phong phú (JSONB, array, UUID).
- MVCC + isolation level chuẩn, window function / CTE mạnh.
- MySQL (InnoDB) vẫn rất phổ biến, đơn giản, đọc nhanh; chọn theo hệ sinh thái của công ty.
- Với JPA, đổi giữa hai loại này chủ yếu là đổi driver + URL + `dialect` (Hibernate tự nhận dialect).

### 4.4 Connection pool (HikariCP)
Tạo kết nối DB rất tốn → Spring Boot dùng **HikariCP** giữ sẵn một pool kết nối (mặc định tối đa 10).
Câu hỏi hay gặp: "Pool quá nhỏ / quá lớn thì sao?" → quá nhỏ: request phải chờ; quá lớn: DB bị quá tải.

---

## 5. Nghiệp vụ Voucher (phần cốt lõi để kể trong phỏng vấn)

`VoucherServiceImpl.canUseVoucher(customer, order, code)` trả `true` khi **tất cả** điều kiện sau đúng:

1. `customer` và `order` khác null.
2. **Order thuộc đúng customer** (`customer.id == order.customerId`) — chống dùng đơn của người khác.
3. Customer không bị `BANNED`.
4. Customer là **VIP** *hoặc* đã có **≥ 3 đơn** (`orderCount >= 3`).
5. Mã voucher không rỗng.
6. Đơn hàng đạt tối thiểu **500.000**.
7. Riêng mã **SALE50**: đơn hàng phải **≥ 2.000.000**.

Còn `checkVoucher(code, amount)` kiểm tra bản thân voucher: `active`, trong khoảng `startAt`–`endAt`,
chưa vượt `usageLimit`, và đạt `minOrderAmount`.

**Kỹ thuật đáng nói:**
- **Fail-fast / early return:** mỗi điều kiện sai thì `return false` ngay, code phẳng, dễ đọc.
- **Tiền tệ dùng `BigDecimal`**, so sánh bằng `compareTo` (không dùng `equals` vì `2.0 != 2.00`).
  Không dùng `double` vì lỗi làm tròn nhị phân (0.1 + 0.2 ≠ 0.3).
- **Null-safe:** `"BANNED".equalsIgnoreCase(status)` (đặt literal bên trái) và `Objects.equals`.
- **Magic number** đã gom thành hằng số `MIN_ORDER_AMOUNT`, `SALE50_MIN_ORDER_AMOUNT`.

---

## 6. Transaction & Concurrency (hỏi rất nhiều)

### 6.1 ACID
- **A**tomicity: hoặc làm hết hoặc không làm gì.
- **C**onsistency: dữ liệu luôn thỏa ràng buộc.
- **I**solation: các transaction song song không làm hỏng nhau.
- **D**urability: commit rồi thì không mất.

### 6.2 `@Transactional`
- Hoạt động qua **proxy (AOP)** → gọi method `@Transactional` từ *chính class đó* (self-invocation) sẽ **không** có transaction.
- Mặc định rollback với `RuntimeException`/`Error`, **không** rollback với checked exception
  (cần `rollbackFor = Exception.class`). Lưu ý: project đang `throws Exception` ở nhiều chỗ.
- `readOnly = true` cho các method chỉ đọc.

### 6.3 Isolation level
`READ COMMITTED` (mặc định PostgreSQL) → `REPEATABLE READ` → `SERIALIZABLE`.
Các hiện tượng: dirty read, non-repeatable read, phantom read.

### 6.4 Race condition với voucher
Hai request cùng dùng voucher còn 1 lượt → cả hai đều đọc `usedCount < usageLimit` → dùng vượt giới hạn.
Cách xử lý:
- **Optimistic lock:** thêm `@Version`; ai commit sau sẽ bị `OptimisticLockException`.
- **Pessimistic lock:** `@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`.
- **Update nguyên tử:** `UPDATE vouchers SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit`.

---

## 7. SQL & Database nền tảng

- **Index:** B-Tree mặc định; tăng tốc `WHERE/JOIN/ORDER BY` nhưng làm chậm ghi và tốn dung lượng.
  `unique = true` (vd. `code`, `order_code`) tự tạo unique index.
  Composite index tuân theo quy tắc *leftmost prefix*.
- **JOIN:** INNER / LEFT / RIGHT / FULL.
- **PRIMARY KEY vs UNIQUE:** PK không NULL và chỉ có 1; UNIQUE có thể nhiều.
- **Chuẩn hóa (Normalization):** tách bảng để tránh dư thừa (1NF, 2NF, 3NF).
- **`DELETE` vs `TRUNCATE` vs `DROP`:** xóa dòng (có WHERE, rollback được) / xóa hết dòng (nhanh) / xóa luôn bảng.
- **`EXPLAIN ANALYZE`** để xem kế hoạch thực thi, phát hiện Seq Scan thừa.

---

## 8. Java nền tảng đã dùng trong project

- **`Optional`:** `findById(id).orElseThrow(() -> new NotFoundException(...))` — tránh `null` tường minh.
- **`final` + constructor:** bất biến, thread-safe cho bean singleton.
- **`Map.of(...)`:** map bất biến, **không chấp nhận null** (chú ý khi build response).
- **`LocalDateTime`** thay `Date` (immutable, thread-safe).
- **Checked vs Unchecked exception:** `NotFoundException`/`IllegalArgumentException` — bản chất để Spring map sang HTTP status.
- **Bean mặc định là singleton** → không được giữ state thay đổi trong service.
- **`Enum`:** project có sẵn `OrderStatus`, `CustomerLevel`... nhưng entity đang lưu `String` (xem mục 9).

---

## 9. Điểm có thể cải thiện (rất hay bị hỏi: "nếu làm lại bạn sẽ cải thiện gì?")

| Vấn đề hiện tại | Hướng cải thiện |
|---|---|
| `Order.customerId` là `Long` thuần | Dùng `@ManyToOne Customer` để có FK thật và toàn vẹn dữ liệu |
| `status`, `level` lưu `String` | Dùng `enum` + `@Enumerated(EnumType.STRING)` để tránh gõ sai giá trị |
| `orderCode = "ORD-" + currentTimeMillis()` | Có thể trùng khi 2 request cùng mili-giây → dùng sequence/UUID |
| Chưa có `@Transactional` ở service | Thêm cho các thao tác ghi nhiều bước |
| `throws Exception` chung chung | Dùng exception cụ thể (unchecked) |
| Entity trả thẳng ra API | Dùng DTO response để không lộ cấu trúc DB |
| Validate thủ công `if` | Dùng Bean Validation (`@Valid`, `@NotNull`, `@Min`) |
| `ddl-auto=update` | Production dùng Flyway/Liquibase + `validate` |
| Mật khẩu DB mặc định trong properties | Đọc từ biến môi trường / secret manager |
| Chưa có test nghiệp vụ voucher | Viết unit test cho từng nhánh điều kiện (JUnit 5 + Mockito) |

---

## 10. Bộ câu hỏi phỏng vấn tự luyện

1. Spring Boot khác Spring Framework ở điểm nào? (auto-config, starter, embedded server)
2. IoC và DI là gì? Vì sao nên dùng constructor injection?
3. Bean scope mặc định là gì? Singleton có an toàn đa luồng không?
4. `@Component`, `@Service`, `@Repository` khác nhau gì?
5. JPA và Hibernate khác nhau thế nào? Lazy loading là gì, N+1 là gì, sửa ra sao?
6. `save()` của JpaRepository xử lý insert/update như thế nào?
7. `@Transactional` hoạt động ra sao? Khi nào nó "không chạy"?
8. Vì sao tiền phải dùng `BigDecimal`? `equals` vs `compareTo`?
9. Làm sao tránh 2 người cùng dùng 1 lượt voucher cuối cùng?
10. Vì sao đổi H2 sang PostgreSQL? Production dùng `ddl-auto` gì?
11. Index là gì? Khi nào index không được dùng?
12. REST là gì? Phân biệt GET/POST/PUT/PATCH/DELETE, tính idempotent.
13. HTTP status 400 / 404 / 409 / 500 dùng khi nào?
14. Giải thích luồng một request đi từ browser đến DB và quay lại.
15. Nếu làm lại project này bạn cải thiện điều gì? (dùng bảng ở mục 9)

---

## 11. Cách trả lời phỏng vấn về project này (khung 60 giây)

> "Em làm một REST API bằng Spring Boot theo kiến trúc 3 lớp Controller – Service – Repository, dùng Spring Data JPA
> với PostgreSQL. Phần em tâm đắc là nghiệp vụ voucher: kiểm tra đơn có thuộc khách hàng không, khách có bị khóa không,
> là VIP hoặc đủ 3 đơn, đơn đạt giá trị tối thiểu và quy tắc riêng cho từng mã. Em dùng `BigDecimal` cho tiền,
> `GlobalExceptionHandler` để trả lỗi nhất quán. Em cũng đã chuyển DB từ H2 sang PostgreSQL bằng Docker Compose.
> Nếu có thêm thời gian em sẽ thêm `@Transactional`, khóa lạc quan chống dùng voucher quá giới hạn, DTO và Flyway."

---

*File này được tạo tự động bởi nút "Tổng hợp kiến thức" trên trang chủ. Mỗi lần bấm sẽ tạo một file mới theo ngày giờ.*
