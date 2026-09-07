package com.langly.langly_backend.service;

import com.langly.langly_backend.model.Article;
import com.langly.langly_backend.model.RefreshToken;
import com.langly.langly_backend.model.SavedArticle;
import com.langly.langly_backend.model.User;
import com.langly.langly_backend.repository.ArticleRepository;
import com.langly.langly_backend.repository.SavedArticleRepository;
import com.langly.langly_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SavedArticleService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final SavedArticleRepository savedArticleRepository;

    public SavedArticleService(UserRepository userRepository,
                               ArticleRepository articleRepository,
                               SavedArticleRepository savedArticleRepository) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.savedArticleRepository = savedArticleRepository;
    }

    public boolean toggle(String email, Long articleId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        Optional<SavedArticle> existing = savedArticleRepository.findByUserAndArticle(user, article);

        if (existing.isPresent()) {
                savedArticleRepository.delete(existing.get());
                return false;
        } else {
            SavedArticle savedArticle = new SavedArticle(user,article);
            savedArticleRepository.save(savedArticle);
            return true;
        }
    }
}