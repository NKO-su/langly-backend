package com.langly.langly_backend.repository;

import com.langly.langly_backend.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    boolean existsByLink(String link);
}
