package com.example.test.service;

import java.nio.file.Path;

public interface KnowledgeService {

    /** Tạo file tổng hợp kiến thức trong thư mục knowledge, trả về đường dẫn file. */
    Path generateSummary() throws Exception;
}
