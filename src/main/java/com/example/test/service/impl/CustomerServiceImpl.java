package com.example.test.service.impl;

import com.example.test.entity.Customer;
import com.example.test.exception.NotFoundException;
import com.example.test.repository.CustomerRepository;
import com.example.test.request.CustomerRequest;
import com.example.test.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "BANNED");
    private static final Set<String> LEVELS = Set.of("NORMAL", "VIP");

    private final CustomerRepository customerRepository;

    // Dùng constructor injection thay cho @Autowired field injection
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("Customer id must not be null");
        }

        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    @Override
    public Customer createCustomer(CustomerRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Customer request must not be null");
        }

        String name = request.getName() == null ? null : request.getName().trim();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("Customer phone is required");
        }

        Customer customer = new Customer();
        customer.setName(name);
        customer.setPhone(phone);
        customer.setAge(request.getAge());

        // Không truyền thì giữ mặc định trong entity: status = ACTIVE, level = NORMAL, orderCount = 0
        if (request.getStatus() != null) {
            customer.setStatus(normalize(request.getStatus(), STATUSES, "status"));
        }
        if (request.getLevel() != null) {
            customer.setLevel(normalize(request.getLevel(), LEVELS, "level"));
        }
        if (request.getOrderCount() != null) {
            if (request.getOrderCount() < 0) {
                throw new IllegalArgumentException("Customer orderCount must be >= 0");
            }
            customer.setOrderCount(request.getOrderCount());
        }

        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(long id) throws NotFoundException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Customer not found: " + id));

        customerRepository.delete(customer);
    }

    private String normalize(String value, Set<String> allowed, String field) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Customer " + field + " must be one of " + allowed + ": " + value);
        }
        return normalized;
    }
}
