# Lời giải – Bài tập `@Transactional`

> **Tự làm trước rồi mới mở file này.**
> Trạng thái kiểm chứng: lời giải Bài 1, 2, 3, 6, 7 đã được chạy thật với PostgreSQL 16 (test xanh; Bài 3 đã chứng minh đỏ khi bỏ `rollbackFor`).
> Bài 0, 4, 5 là bài quan sát/thiết kế, **chưa chạy thử** – nếu kết quả khác, hãy tin kết quả bạn tự thấy rồi tìm hiểu nguyên nhân.

---

## Bài 0

Log mong đợi (rút gọn):
```
Getting transaction for [...CustomerServiceImpl.createCustomer]
... INSERT ...
Completing transaction for [...CustomerServiceImpl.createCustomer]
Getting transaction for [...CustomerServiceImpl.getCustomerById]   (read-only)
```
`OrderServiceImpl.createOrder` không có dòng nào vì chưa có `@Transactional` (mỗi lệnh `save()` tự có transaction nhỏ của
`SimpleJpaRepository`, nhưng cả use-case thì **không** nằm trong một transaction).

Trả lời tự kiểm tra:
1. Proxy (CGLIB) tạo ra bằng cách kế thừa class và override method; `private` không override được, nên proxy không chặn được.
2. Chưa. `save()` chỉ đưa entity vào persistence context. Câu `INSERT` chạy khi flush (commit, hoặc trước một query).
   Riêng `IDENTITY` thì `INSERT` chạy ngay trong `save()` vì cần lấy id do DB sinh.

---

## Bài 1

`OrderRequest`:
```java
private List<OrderItemRequest> items;
public List<OrderItemRequest> getItems() { return items; }
public void setItems(List<OrderItemRequest> items) { this.items = items; }
```

`OrderServiceImpl` (thêm `OrderItemService` vào constructor):
```java
@Override
@Transactional(rollbackFor = Exception.class)
public Order createOrder(OrderRequest request) throws Exception {
    validate(request);
    boolean hasItems = request.getItems() != null && !request.getItems().isEmpty();

    Order order = new Order();
    order.setCustomerId(request.getCustomerId());
    order.setTotalAmount(hasItems ? BigDecimal.ZERO : request.getTotalAmount());
    order.setPaymentMethod(request.getPaymentMethod());
    order.setShippingAddress(request.getShippingAddress());
    order.setNote(request.getNote());
    order.setOrderCode("ORD-" + System.currentTimeMillis());
    order.setStatus("PENDING");
    order.setCreatedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());

    Order saved = orderRepository.save(order);
    if (hasItems) {
        List<OrderItem> items = orderItemService.createItems(saved.getId(), request.getItems());
        saved.setTotalAmount(orderItemService.calculateTotal(items)); // dirty checking, không cần save lại
    }
    return saved;
}
```
`validate` cũng phải sửa: chỉ bắt buộc `totalAmount` khi **không** có `items`.

Trả lời:
1. `order_items.order_id` là `nullable = false`, cần id của order.
2. `saved` là entity đang được persistence context quản lý; lúc commit Hibernate so với bản gốc, thấy `totalAmount` đổi → `UPDATE`.
3. Item 1 đã trừ kho xong; item 2 lỗi → `createItems` tự rollback nếu nó là transaction gốc (do nó có `@Transactional`),
   **nhưng** `Order` đã được `save()` ở transaction nhỏ riêng và đã commit → còn lại một đơn rỗng. Đây là lý do cần transaction bao ở `createOrder`.

---

## Bài 2 và Bài 3 – `OrderTxTest`

```java
@SpringBootTest   // KHÔNG đặt @Transactional ở đây
class OrderTxTest {
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CustomerRepository customerRepository;

    Product product(int stock) {
        Product p = new Product();
        p.setSku(UUID.randomUUID().toString());       // sku ngẫu nhiên để test chạy lặp được
        p.setName("p");
        p.setPrice(new BigDecimal("100000"));
        p.setStock(stock);
        p.setCreatedAt(LocalDateTime.now());
        return productRepository.save(p);
    }

    OrderItemRequest item(Long productId, int quantity) {
        OrderItemRequest r = new OrderItemRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }

    Long customer() {
        Customer c = new Customer();
        c.setName("t");
        c.setPhone("1");
        return customerRepository.save(c).getId();
    }

    @Test
    void commitWhenAllOk() throws Exception {
        Product a = product(10);
        OrderRequest req = new OrderRequest();
        req.setCustomerId(customer());
        req.setItems(List.of(item(a.getId(), 3)));

        Order o = orderService.createOrder(req);

        assertEquals(7, productRepository.findById(a.getId()).get().getStock());
        assertEquals(0, new BigDecimal("300000")
                .compareTo(orderRepository.findById(o.getId()).get().getTotalAmount()));
    }

    @Test
    void rollbackWholeOrderWhenSecondItemFails() {            // Bài 2: unchecked exception
        Product a = product(10), b = product(1);
        long before = orderRepository.count();
        OrderRequest req = new OrderRequest();
        req.setCustomerId(customer());
        req.setItems(List.of(item(a.getId(), 3), item(b.getId(), 5)));

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(req));

        assertEquals(10, productRepository.findById(a.getId()).get().getStock());
        assertEquals(before, orderRepository.count());
    }

    @Test
    void rollbackWhenSecondProductNotFound() {                // Bài 3: checked exception
        Product a = product(10);
        long before = orderRepository.count();
        OrderRequest req = new OrderRequest();
        req.setCustomerId(customer());
        req.setItems(List.of(item(a.getId(), 3), item(999999L, 1)));

        assertThrows(NotFoundException.class, () -> orderService.createOrder(req));

        assertEquals(10, productRepository.findById(a.getId()).get().getStock());
        assertEquals(before, orderRepository.count());
    }
}
```
Kết quả đã quan sát khi đổi cả 3 chỗ thành `@Transactional` trần: test 3 đỏ với `expected: <10> but was: <7>`
(kho item 1 bị trừ, đơn vẫn commit), còn test 2 vẫn xanh vì `IllegalStateException` là unchecked.

Trả lời Bài 2: (1) Vì sau khi service ném lỗi transaction đã kết thúc; chỉ khi đọc lại từ DB bằng một lượt đọc mới mới biết dữ liệu thật sự
có được giữ hay không. (2) Không: cả test chạy trong một transaction, `findById` thấy ngay thay đổi chưa commit/rollback của chính nó.

Trả lời Bài 3: (1) Quy ước từ EJB: checked = tình huống nghiệp vụ "đã lường trước, caller có thể xử lý tiếp" nên mặc định vẫn commit;
unchecked/Error = lỗi lập trình hay hệ thống nên rollback. (2) Transaction chung: bất kỳ lớp nào trong chuỗi ném
exception ra ngoài cùng đều dựa vào cấu hình của **lớp ngoài cùng đang sở hữu** transaction (ở đây là `createOrder`), nên `createOrder`
là chỗ bắt buộc phải có `rollbackFor`. Đặt đồng bộ ở cả chuỗi là an toàn nhất.

Gợi ý Bài 3.4: đổi `NotFoundException` thành `extends RuntimeException` giúp rollback mặc định đúng và bỏ được `throws Exception`
khắp nơi; đổi lại là caller không bị compiler nhắc xử lý. Phần lớn dự án Spring chọn unchecked.

---

## Bài 4

Kết quả: `outer()` in `tx active = false`; gọi `inner()` trực tiếp từ test/controller in `tx active = true`.
`outer()` gọi `this.inner()` nên không đi qua proxy.

Cách sửa tùy ý: (a) thêm `@Transactional` lên `outer()`; (b) chuyển `inner()` sang bean khác và inject vào.

Trả lời: (1) Không – `createOrder` gọi `orderItemService` là **bean khác** được inject vào, nên gọi qua proxy của bean đó.
(2) Proxy CGLIB cần kế thừa/override; `final`/`private` không override được.

---

## Bài 5

```java
@Entity @Table(name = "order_audit")
public class OrderAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long customerId;
    private String result;                // SUCCESS | FAILED
    @Column(length = 500) private String message;
    private LocalDateTime createdAt;
    // getter/setter
}

public interface OrderAuditRepository extends JpaRepository<OrderAudit, Long> {}

@Service
public class OrderAuditService {
    private final OrderAuditRepository repo;
    public OrderAuditService(OrderAuditRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)   // transaction riêng, commit độc lập
    public void record(Long customerId, String result, String message) {
        OrderAudit a = new OrderAudit();
        a.setCustomerId(customerId);
        a.setResult(result);
        a.setMessage(message);
        a.setCreatedAt(LocalDateTime.now());
        repo.save(a);
    }
}
```
Trong `createOrder`:
```java
try {
    // ... toàn bộ nghiệp vụ như Bài 1 ...
    return saved;
} catch (Exception e) {
    auditService.record(request.getCustomerId(), "FAILED", e.getMessage());
    throw e;                       // phải ném lại để transaction cha rollback
}
```
Test: gọi đơn lỗi (kho không đủ) → `orderRepository.count()` không đổi, `orderAuditRepository.count()` tăng 1.
Đổi sang `REQUIRED`: audit nằm trong transaction cha nên bị rollback theo → không còn dòng audit.

Trả lời: (1) Mỗi `REQUIRES_NEW` lồng nhau cần thêm một connection trong khi cái cha vẫn giữ connection của nó → pool cạn, các thread
chờ nhau (deadlock mức pool). (2) Transaction mới độc lập; nó chỉ bảo vệ dữ liệu **của chính nó** (audit), không cứu được dữ liệu của cha.

---

## Bài 6

```java
@SpringBootTest
class ConcurrencyTest {
    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;

    @Test
    void stockStaysConsistentUnderConcurrency() throws Exception {
        Product p = new Product();
        p.setSku(UUID.randomUUID().toString());
        p.setName("p");
        p.setPrice(BigDecimal.TEN);
        p.setStock(20);
        p.setCreatedAt(LocalDateTime.now());
        Long id = productRepository.save(p).getId();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), conflict = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();                                   // cùng xuất phát
                try {
                    productService.decreaseStock(id, 1);
                    ok.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    conflict.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        int stock = productRepository.findById(id).get().getStock();
        assertEquals(threads, ok.get() + conflict.get());
        assertEquals(20 - ok.get(), stock);
    }
}
```
Một lần chạy thật cho kết quả `OK=1 CONFLICT=9 stock=19` – tức là 9/10 request thua vì xung đột. Số liệu **luôn nhất quán** (kho không
bao giờ bị trừ sai), cái giá là phải để client thử lại.

Trả lời: (1) Không có `@Version`: nhiều thread cùng đọc 20 và cùng ghi 19 → kho cuối lớn hơn `20 - ok` (mất lượt trừ, tức bán lố hàng).
(2) Không tốt: hầu hết request thất bại. Đổi sang: retry có giới hạn, `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`),
hoặc UPDATE nguyên tử `SET stock = stock - ? WHERE id = ? AND stock >= ?`.
(3) `@Version` được kiểm khi Hibernate **flush** – thường là lúc commit, nằm ở proxy sau khi thân hàm service đã chạy xong;
vì vậy `try/catch` bên trong thân hàm không bắt được, phải xử lý ở nơi gọi (hoặc `@ControllerAdvice` như project đã làm).

---

## Bài 7

`VoucherRepository`:
```java
@Modifying
@Query("update Voucher v set v.usedCount = v.usedCount + 1 "
     + "where v.id = :id and (v.usageLimit is null or v.usedCount < v.usageLimit)")
int increaseUsedCount(@Param("id") Long id);
```
`VoucherServiceImpl`:
```java
@Transactional
public void useVoucher(String code) throws Exception {
    Voucher voucher = getVoucherByCode(code);
    if (voucherRepository.increaseUsedCount(voucher.getId()) == 0) {
        throw new IllegalStateException("Voucher đã hết lượt");
    }
}
```
Test (20 thread, `usageLimit = 5`; dùng `TransactionTemplate` để mỗi thread có transaction riêng):
```java
Integer n = tx.execute(s -> voucherRepository.increaseUsedCount(id));
if (n == 1) ok.incrementAndGet();
...
assertEquals(5, ok.get());
assertEquals(5, voucherRepository.findById(id).get().getUsedCount());
```
Đã chạy thật: đúng 5 thread thành công, `usedCount` cuối = 5.

Trả lời: (1) Giữa lúc đọc `usedCount` và lúc ghi có khoảng hở; nhiều thread cùng thấy `4 < 5` rồi cùng ghi `5` hoặc vượt giới hạn.
(2) `TransactionRequiredException: Executing an update/delete query`. (3) Có – entity trong persistence context vẫn giữ giá trị cũ;
dùng `@Modifying(clearAutomatically = true)` hoặc `entityManager.refresh(...)` / tải lại.
