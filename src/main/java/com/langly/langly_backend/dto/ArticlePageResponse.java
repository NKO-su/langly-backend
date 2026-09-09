package com.langly.langly_backend.dto;

import com.langly.langly_backend.model.Article;

import java.util.List;

public class ArticlePageResponse {
    private List<Article> articles;
    private Long nextCursor;
    private boolean hasMore;

    public ArticlePageResponse(List<Article> articles, Long nextCursor, boolean hasMore) {
        this.articles = articles;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public ArticlePageResponse() {

    }

    public List<Article> getArticles() {
        return articles;
    }

    public Long getNextCursor() {
        return nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    public void setNextCursor(Long nextCursor) {
        this.nextCursor = nextCursor;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
