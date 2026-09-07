package com.langly.langly_backend.controller;

import com.langly.langly_backend.dto.SavedStatusResponse;
import com.langly.langly_backend.dto.ToggleSaveRequest;
import com.langly.langly_backend.service.SavedArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saved-articles")
public class SavedArticleController {

    private final SavedArticleService savedArticleService;

    public SavedArticleController(SavedArticleService savedArticleService) {
        this.savedArticleService = savedArticleService;
    }

    @PostMapping("/toggle")
    public ResponseEntity<SavedStatusResponse> toggle(
            @RequestBody ToggleSaveRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        boolean saved = savedArticleService.toggle(email, request.getArticleId());
        return ResponseEntity.ok(new SavedStatusResponse(saved));
    }
}

