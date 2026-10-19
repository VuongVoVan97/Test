package com.example.test.service.impl;

import com.example.test.repository.CategoryRepository;
import com.example.test.repository.CustomerRepository;
import com.example.test.repository.OrderItemRepository;
import com.example.test.repository.OrderRepository;
import com.example.test.repository.ProductRepository;
import com.example.test.repository.VoucherRepository;
import com.example.test.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final String TEMPLATE = "knowledge/knowledge-template.md";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final DataSource dataSource;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VoucherRepository voucherRepository;
    private final Path knowledgeDir;

    public KnowledgeServiceImpl(
            DataSource dataSource,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            VoucherRepository voucherRepository,
            @Value("${app.knowledge.dir:knowledge}") String knowledgeDir) {
        this.dataSource = dataSource;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.voucherRepository = voucherRepository;
        this.knowledgeDir = Path.of(knowledgeDir);
    }

    @Override
    public Path generateSummary() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        String content = buildHeader(now) + readTemplate();

        Files.createDirectories(knowledgeDir);
        Path file = knowledgeDir.resolve("knowledge_" + now.format(FILE_TIME) + ".md");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private String buildHeader(LocalDateTime now) throws Exception {
        return "# Tổng hợp kiến thức - " + now.format(DISPLAY_TIME) + "\n\n"
                + "## 0. Trạng thái project tại thời điểm tổng hợp\n\n"
                + "- Database đang dùng: **" + databaseName() + "**\n"
                + "- Số bản ghi: customers = " + customerRepository.count()
                + ", orders = " + orderRepository.count()
                + ", order_items = " + orderItemRepository.count()
                + ", products = " + productRepository.count()
                + ", categories = " + categoryRepository.count()
                + ", vouchers = " + voucherRepository.count() + "\n\n"
                + "---\n\n";
    }

    private String databaseName() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        }
    }

    private String readTemplate() throws Exception {
        try (var in = new ClassPathResource(TEMPLATE).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
