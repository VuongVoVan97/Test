package com.example.test.controller;

import com.example.test.service.KnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/summary")
    public ResponseEntity<Map<String, Object>> generateSummary() throws Exception {
        Path file = knowledgeService.generateSummary();

        return ResponseEntity.ok(Map.of(
                "fileName", file.getFileName().toString(),
                "path", file.toAbsolutePath().toString(),
                "sizeBytes", Files.size(file)
        ));
    }
}
