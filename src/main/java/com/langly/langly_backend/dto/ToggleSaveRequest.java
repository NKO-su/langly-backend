package com.langly.langly_backend.dto;

public class ToggleSaveRequest {
    private Long articleId;

    public ToggleSaveRequest() {

    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }
}
