package com.langly.langly_backend.repository;

import com.langly.langly_backend.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    boolean existsByLink(String link);

    List<Article> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

}
