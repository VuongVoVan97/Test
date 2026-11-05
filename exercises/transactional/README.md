# Bài tập `@Transactional` – vừa học lý thuyết vừa làm

Mỗi bài có 4 phần: **Lý thuyết → Việc cần làm → Cách kiểm tra → Câu hỏi tự kiểm tra**.
Làm lần lượt từ Bài 0 đến Bài 7; bài sau dùng kết quả của bài trước.
Lời giải nằm ở [`SOLUTIONS.md`](SOLUTIONS.md) – **tự làm trước, đừng mở sớm**.

> Chuẩn bị: `docker compose up -d` (PostgreSQL), `./mvnw spring-boot:run`.
> Test chạy với PostgreSQL đang bật: `./mvnw test -Dtest=TenTest`.

---

## Đọc code hiện tại – những gì cần biết trước khi làm

| Chỗ | Hiện trạng | Liên quan bài |
|---|---|---|
| `OrderServiceImpl.createOrder` | Chỉ lưu `Order`, **không** tạo `OrderItem`, **không** trừ kho, **không** có `@Transactional`. Tổng tiền do client gửi lên. | Bài 1 |
| `OrderItemServiceImpl.createItems` | Đã có `@Transactional(rollbackFor = Exception.class)`, trừ kho từng sản phẩm, nhưng **không ai gọi** hàm này (`OrderItemController` chỉ có GET). | Bài 1 |
| `ProductServiceImpl.decreaseStock/increaseStock` | Có `@Transactional(rollbackFor = Exception.class)`; `Product` có `@Version` (optimistic lock). | Bài 3, 6 |
| `GlobalExceptionHandler` | Đã bắt `ObjectOptimisticLockingFailureException` → 409. | Bài 6 |
| `CustomerServiceImpl` | `@Transactional` ở cả class, `readOnly = true` cho hàm đọc. | Bài 0 |
| `Voucher.usedCount` | **Không ở đâu tăng** → voucher không bao giờ hết lượt. | Bài 7 |
| `Customer.orderCount` | Không ở đâu tăng → điều kiện "≥ 3 đơn" của voucher không bao giờ đạt tự nhiên. | Bài 1 (mở rộng) |

---

## Bài 0 – Nhìn tận mắt transaction bắt đầu và kết thúc

### Lý thuyết
- **Transaction** = nhóm thao tác DB "được cả, mất cả" (ACID: Atomicity, Consistency, Isolation, Durability).
- Spring không sửa code của bạn. Khi bean có `@Transactional`, Spring tạo một **proxy** bọc quanh bean:
  ```
  Controller ──gọi──▶ [Proxy] ──▶ Service thật
                        │ trước: begin (lấy connection, setAutoCommit(false))
                        │ sau  : commit, hoặc rollback nếu có exception
  ```
- Hibernate không ghi ngay khi bạn `save()`; nó **gom thay đổi** (persistence context) và **flush** xuống DB khi commit
  (hoặc trước một câu query). Entity đang được quản lý mà bạn `set...()` thì tự được UPDATE lúc commit – gọi là **dirty checking**
  (vì vậy `decreaseStock` không cần gọi `save()`).
- `readOnly = true`: gợi ý cho Hibernate bỏ dirty checking → nhanh hơn, và an toàn hơn cho hàm chỉ đọc.

### Việc cần làm
1. Thêm vào `application.properties`:
   ```properties
   logging.level.org.springframework.transaction.interceptor=TRACE
   logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
   ```
2. Chạy app, trên trang chủ tạo 1 khách hàng (gọi `POST /customer`), rồi gọi `GET /customer/{id}`.
3. Đọc log, tìm các dòng `Getting transaction for [...]` và `Completing transaction for [...]`.

### Cách kiểm tra
Bạn phải chỉ ra được trong log: (a) transaction mở ở `CustomerServiceImpl.createCustomer` và commit khi hàm kết thúc;
(b) `getCustomerById` mở transaction **readOnly**; (c) `OrderServiceImpl.createOrder` **không** có dòng nào → chưa có transaction.

### Tự kiểm tra
1. Vì sao `@Transactional` đặt trên `private` method không có tác dụng?
2. `save()` có nghĩa là đã ghi xuống DB chưa? Khi nào mới thực sự `INSERT`?
3. Với `GenerationType.IDENTITY`, `INSERT` xảy ra lúc nào? (gợi ý: cần id ngay)

---

## Bài 1 – Atomicity: tạo đơn hàng + trừ kho là MỘT transaction

### Lý thuyết
- **Propagation `REQUIRED`** (mặc định): nếu đã có transaction thì **tham gia** nó, chưa có thì tạo mới.
  `createOrder` (ngoài) → `createItems` → `decreaseStock` (trong) đều dùng chung **một** transaction,
  nên chỉ cần một bước lỗi là **toàn bộ** rollback.
- Quy tắc ranh giới: đặt `@Transactional` ở **tầng Service**, ở method đại diện cho **một use-case trọn vẹn**
  ("đặt hàng"), không đặt ở Controller, và không nên chỉ đặt ở các hàm con.
- **Không** tin dữ liệu tiền từ client: tổng tiền phải do server tính từ giá sản phẩm (snapshot trong `OrderItem`).

### Việc cần làm
1. `OrderRequest` thêm `List<OrderItemRequest> items` (+ getter/setter).
2. Sửa `OrderServiceImpl.createOrder`:
   - Có `@Transactional(rollbackFor = Exception.class)`.
   - Lưu `Order` với tổng tạm `BigDecimal.ZERO`, rồi gọi `orderItemService.createItems(order.getId(), request.getItems())`.
   - Đặt `totalAmount = orderItemService.calculateTotal(items)`.
   - Nếu `items` rỗng thì giữ hành vi cũ (dùng `totalAmount` client gửi) để không vỡ trang chủ.
3. *(Mở rộng)* Tăng `customer.orderCount` trong cùng transaction.

### Cách kiểm tra
Tạo product (qua DB hoặc test), gọi `POST /order` kèm `items`, kiểm tra: `orders` có thêm 1 dòng,
`order_items` đúng số dòng, `products.stock` giảm, `orders.total_amount` = tổng `subtotal`.

### Tự kiểm tra
1. Vì sao lưu `Order` trước rồi mới tạo item? (cần `order.id`)
2. Dòng `saved.setTotalAmount(...)` sau `save()` không gọi `save()` lại mà vẫn được lưu – vì sao?
3. Nếu bỏ `@Transactional` ở `createOrder` nhưng `createItems` vẫn có, chuyện gì xảy ra khi item thứ 2 lỗi?

---

## Bài 2 – Chứng minh rollback bằng test (và cái bẫy `@Transactional` trong test)

### Lý thuyết
- Một tính năng transaction chỉ "đúng" khi bạn **đã thấy nó rollback**. Viết test cho nhánh lỗi.
- **Bẫy kinh điển:** nếu gắn `@Transactional` lên class test, Spring bọc cả test trong 1 transaction rồi rollback cuối test →
  service bên trong **tham gia** transaction đó, không bao giờ commit/rollback thật → test xanh dù code sai.
  Vì vậy test này **không** để `@Transactional`; tự dọn dữ liệu (dùng `sku` ngẫu nhiên) hoặc so sánh số lượng trước/sau.

### Việc cần làm
Tạo `src/test/java/com/example/test/OrderTxTest.java` (`@SpringBootTest`) với 2 test:
1. **Thành công:** product tồn kho 10, mua 3 → stock còn 7, `total_amount` = 3 × giá.
2. **Thất bại giữa chừng:** đơn có 2 item – item 1 hợp lệ (mua 3 / kho 10), item 2 mua vượt kho (mua 5 / kho 1).
   Mong đợi `IllegalStateException`; **stock của item 1 vẫn là 10** và số dòng `orders` không đổi.

### Cách kiểm tra
`./mvnw test -Dtest=OrderTxTest` xanh cả 2. Rồi **phá thử**: tạm xóa dòng `@Transactional` ở `createOrder` và `createItems`
→ test 2 phải đỏ (kho của item 1 đã bị trừ). Nhớ khôi phục.

### Tự kiểm tra
1. Vì sao trong test 2 phải kiểm tra bằng `repository.findById` *sau* khi service ném lỗi, thay vì kiểm tra trong cùng hàm service?
2. Nếu thêm `@Transactional` lên class test, test 2 còn phát hiện được lỗi thiếu rollback không?

---

## Bài 3 – Bẫy checked exception: rollback mặc định KHÔNG xảy ra

### Lý thuyết
Quy tắc mặc định của Spring: rollback khi gặp **`RuntimeException` và `Error`**; **không** rollback với checked exception
(những class `extends Exception` mà không `extends RuntimeException`).
Project có `NotFoundException` là *checked* và nhiều hàm `throws Exception` → nên mới cần `rollbackFor = Exception.class`
(xem comment `[REVIEW FIX]` trong `OrderItemServiceImpl`).

| Exception ném ra | Mặc định | Với `rollbackFor = Exception.class` |
|---|---|---|
| `IllegalStateException` (unchecked) | rollback | rollback |
| `NotFoundException` (checked) | **commit!** | rollback |

### Việc cần làm
1. Thêm test thứ 3 vào `OrderTxTest`: đơn có item 1 hợp lệ, item 2 có `productId = 999999` → mong đợi `NotFoundException`,
   stock item 1 vẫn 10 và không có đơn mới.
2. Chạy → xanh. Sau đó đổi `@Transactional(rollbackFor = Exception.class)` thành `@Transactional` ở **cả** `createOrder`,
   `createItems`, `decreaseStock` và chạy lại.
3. Ghi lại thông báo lỗi bạn thấy (`expected: <10> but was: <7>`) – đó chính là bug "mất kho" trong thực tế.
4. Khôi phục `rollbackFor`. Suy nghĩ: có nên đổi `NotFoundException` thành `extends RuntimeException` không? Ưu/nhược?

### Cách kiểm tra
Test 3 xanh khi có `rollbackFor`, đỏ khi bỏ — bạn đã chứng minh được quy tắc bằng thực nghiệm.

### Tự kiểm tra
1. Vì sao với `Error` hoặc `RuntimeException` thì rollback còn checked thì không? (lịch sử EJB: checked = lỗi nghiệp vụ "đã lường trước")
2. Nếu chỉ `createOrder` có `rollbackFor` mà `decreaseStock` không có thì sao? (gợi ý: transaction chung, ai ném ra ngoài cùng quyết định)

---

## Bài 4 – Bẫy self-invocation (gọi chính class mình)

### Lý thuyết
`@Transactional` chạy qua **proxy**. Gọi `this.method()` trong cùng class đi **thẳng** vào object thật, **bỏ qua proxy**
→ annotation trên `method` bị bỏ qua.
```
Controller → [Proxy] → ServiceImpl.a()
                            └── this.b()   ← không qua proxy, b() KHÔNG có transaction riêng
```
Cách khắc phục: tách `b()` sang bean khác; hoặc đặt `@Transactional` ở hàm ngoài cùng (`a()`);
(tự inject chính mình / `AopContext` là cách xấu, tránh dùng).

### Việc cần làm
1. Trong `OrderServiceImpl` thêm hàm test tạm:
   ```java
   public void outer() {          // KHÔNG có @Transactional
       inner();                    // self-invocation
   }
   @Transactional
   public void inner() {
       System.out.println("tx active = "
           + org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
   }
   ```
2. Gọi `orderService.outer()` (viết 1 test hoặc endpoint tạm) và đọc kết quả `tx active = ...`.
3. Gọi trực tiếp `orderService.inner()` từ bên ngoài và so sánh.
4. Xóa hàm tạm sau khi xong.

### Cách kiểm tra
`outer()` in `false`; gọi `inner()` trực tiếp in `true`. Bạn tự giải thích được vì sao khác nhau.

### Tự kiểm tra
1. Ở project này, `createOrder` gọi `orderItemService.createItems` – có bị self-invocation không? Vì sao không?
2. Vì sao `@Transactional` trên `final` class/method hoặc `private` method cũng không hoạt động?

---

## Bài 5 – Propagation: `REQUIRES_NEW` cho nhật ký audit

### Lý thuyết
| Propagation | Ý nghĩa | Dùng khi |
|---|---|---|
| `REQUIRED` (mặc định) | Tham gia transaction hiện có, không có thì tạo | Hầu hết trường hợp |
| `REQUIRES_NEW` | **Luôn tạo transaction mới**, tạm treo cái cũ | Ghi log/audit phải tồn tại dù nghiệp vụ lỗi |
| `NESTED` | Savepoint trong transaction cha | Rollback một phần (cần JDBC savepoint) |
| `SUPPORTS` / `NOT_SUPPORTED` / `NEVER` / `MANDATORY` | Ít dùng | — |

Cái bẫy **rollback-only**: inner `REQUIRED` ném exception → transaction chung bị đánh dấu *rollback-only*.
Dù outer `catch` được exception đó và chạy tiếp, lúc commit vẫn nổ `UnexpectedRollbackException`.

### Việc cần làm
1. Tạo entity `OrderAudit` (`id`, `customerId`, `result` = SUCCESS/FAILED, `message`, `createdAt`) + repository.
2. Tạo `OrderAuditService.record(...)` với `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
   *(Phải là bean riêng – xem Bài 4!)*
3. Trong `createOrder`: bọc phần nghiệp vụ bằng `try/catch`; ở `catch` gọi `auditService.record(FAILED, ...)` rồi `throw` lại.
4. Test: tạo đơn thất bại (kho không đủ) → `orders` không đổi **nhưng `order_audit` có thêm 1 dòng FAILED**.
5. Thử đổi sang `REQUIRED` và quan sát audit biến mất cùng rollback.

### Cách kiểm tra
Với `REQUIRES_NEW`: audit còn, đơn không có. Với `REQUIRED`: cả hai đều mất.

### Tự kiểm tra
1. Điều gì xảy ra với connection pool khi lồng `REQUIRES_NEW` rất sâu? (mỗi cấp giữ thêm 1 connection → có thể cạn pool / deadlock)
2. Vì sao `REQUIRES_NEW` không "cứu" được dữ liệu nghiệp vụ của transaction cha?

---

## Bài 6 – Concurrency 1: optimistic lock (`@Version`) đã có sẵn

### Lý thuyết
**Lost update:** hai request cùng đọc stock = 20, cùng ghi 19 → mất một lượt trừ.
- **Optimistic lock** (`@Version`): `UPDATE products SET stock=?, version=version+1 WHERE id=? AND version=?`.
  Nếu ai đó đã đổi trước → 0 dòng bị ảnh hưởng → Hibernate ném `ObjectOptimisticLockingFailureException`.
  Phù hợp khi **xung đột hiếm**; không giữ khóa DB → rẻ. Client nhận 409 và thử lại (đã có handler).
- **Pessimistic lock** (`SELECT ... FOR UPDATE`): chặn người khác ngay từ lúc đọc; hợp khi xung đột nhiều, nhưng dễ deadlock/chờ lâu.
- **Isolation level** mặc định của PostgreSQL là `READ COMMITTED`: mỗi câu lệnh thấy dữ liệu đã commit tại thời điểm đó.

### Việc cần làm
Viết `ConcurrencyTest`: 1 product stock = 20; 10 thread cùng gọi `productService.decreaseStock(id, 1)` (dùng
`ExecutorService` + `CountDownLatch` để các thread xuất phát cùng lúc). Đếm số lần thành công và số lần bị
`ObjectOptimisticLockingFailureException`.

### Cách kiểm tra
Test khẳng định: `thanhCong + xungDot == 10` và `stockCuoi == 20 - thanhCong` (không bao giờ sai lệch).
In ra kết quả; bạn sẽ thấy **rất nhiều** xung đột (ví dụ chỉ 1–3 thread thành công).

### Tự kiểm tra
1. Nếu xóa `@Version` khỏi `Product`, chạy lại test: số liệu sai thế nào?
2. Với flash-sale (xung đột cực nhiều), optimistic lock có còn tốt không? Bạn đổi sang giải pháp nào? (retry, pessimistic, hoặc UPDATE nguyên tử – Bài 7)
3. Vì sao ngoại lệ lock này có thể xuất hiện **ở lúc commit**, tức là ngoài thân hàm service?

---

## Bài 7 – Concurrency 2: UPDATE nguyên tử cho lượt dùng voucher

### Lý thuyết
Chỉ cần **một câu SQL** vừa kiểm tra điều kiện vừa tăng, DB tự khóa dòng đó trong lúc update:
```sql
UPDATE vouchers SET used_count = used_count + 1
WHERE id = ? AND (usage_limit IS NULL OR used_count < usage_limit)
```
Số dòng bị ảnh hưởng: `1` = giành được lượt, `0` = hết lượt. Không cần `@Version`, không cần retry, không có cửa sổ "đọc rồi mới ghi".
Đây là kiểu **check-then-act làm thành 1 thao tác nguyên tử** – mẫu rất hay được hỏi.

### Việc cần làm
1. Trong `VoucherRepository` thêm `@Modifying @Query(...) int increaseUsedCount(@Param("id") Long id)` (JPQL tương đương câu SQL trên).
2. Trong `VoucherServiceImpl` thêm `useVoucher(String code)` có `@Transactional`: tìm voucher theo code,
   gọi `increaseUsedCount`; nếu trả về `0` → ném `IllegalStateException("Voucher đã hết lượt")`.
3. Nối vào `createOrder` (Bài 1): nếu request có `voucherCode` thì gọi `useVoucher` **trong cùng transaction** –
   đơn lỗi thì lượt voucher cũng không bị tiêu.
4. Test: voucher `usageLimit = 5`, 20 thread cùng tăng → đúng 5 thread thấy `1`, `usedCount` cuối = 5.

### Cách kiểm tra
Chạy test 10 lần liên tiếp đều xanh (đây là loại bug chỉ lộ khi chạy lặp).

### Tự kiểm tra
1. Vì sao cách "đọc `usedCount`, `if < limit`, rồi `+1`, `save`" là sai khi chạy song song?
2. `@Modifying` bắt buộc phải chạy trong transaction – nếu thiếu sẽ báo lỗi gì? (`TransactionRequiredException`)
3. Sau `@Modifying` query, entity `Voucher` đã load trước đó trong persistence context có bị cũ không? (có → `clearAutomatically = true`)

---

## Phụ lục – Bảng tóm tắt để ôn phỏng vấn

| Chủ đề | Nhớ một câu |
|---|---|
| Cơ chế | Proxy AOP bọc bean; begin → chạy → commit/rollback. |
| Rollback mặc định | Chỉ `RuntimeException`/`Error`; checked cần `rollbackFor`. |
| Self-invocation | `this.x()` bỏ qua proxy → mất `@Transactional`. |
| Propagation | `REQUIRED` mặc định; `REQUIRES_NEW` cho audit; coi chừng rollback-only. |
| Đặt ở đâu | Tầng Service, ở method = 1 use-case. Không đặt ở Controller. |
| `readOnly` | Tối ưu cho hàm đọc; không phải ràng buộc cứng. |
| Test | Đừng để `@Transactional` ở test khi muốn kiểm tra commit/rollback thật. |
| Concurrency | `@Version` (xung đột hiếm) / `FOR UPDATE` (xung đột nhiều) / UPDATE nguyên tử (counter). |
| Dirty checking | Entity được quản lý đổi field là tự UPDATE lúc commit, không cần `save()`. |
| Transaction dài | Đừng gọi API ngoài / `sleep` trong transaction – giữ connection và khóa. |

**5 câu hỏi hay gặp:** (1) `@Transactional` hoạt động thế nào? (2) Khi nào nó không rollback? (3) `REQUIRED` vs `REQUIRES_NEW`?
(4) Làm sao ngăn bán vượt tồn kho? (5) Vì sao test có `@Transactional` có thể che bug?
