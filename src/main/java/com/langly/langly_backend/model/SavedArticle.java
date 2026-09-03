package com.langly.langly_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_articles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"})
)
public class SavedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime savedAt;

    public SavedArticle() {
    }

    public SavedArticle(User user, Article article) {
        this.user = user;
        this.article = article;
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }

    public LocalDateTime getSavedAt() { return savedAt; }
}