package com.langly.langly_backend.service;

import com.langly.langly_backend.dto.ArticlePageResponse;
import com.langly.langly_backend.model.Article;
import com.langly.langly_backend.repository.ArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public ArticlePageResponse getArticles(Long cursor, int size) {
        Long effectiveCursor = (cursor != null) ? cursor : Long.MAX_VALUE;
        Pageable limit = PageRequest.of(0, size);

        List<Article> articles = articleRepository
                .findByIdLessThanOrderByIdDesc(effectiveCursor, limit);

        Long nextCursor = articles.isEmpty() ? null : articles.get(articles.size() - 1).getId();

        boolean hasMore = articles.size() == size;

        return new ArticlePageResponse(articles, nextCursor, hasMore);
    }
}