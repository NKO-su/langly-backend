package com.langly.langly_backend.repository;

import com.langly.langly_backend.model.Article;
import com.langly.langly_backend.model.SavedArticle;
import com.langly.langly_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {
    boolean existsByUserAndArticle(User user, Article article);
    List<SavedArticle> findByUser(User user);
    Optional<SavedArticle> findByUserAndArticle(User user, Article article);
}
