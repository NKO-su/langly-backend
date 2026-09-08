package com.langly.langly_backend.service;

import com.langly.langly_backend.model.Article;
import com.langly.langly_backend.repository.ArticleRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class RssFeedService {

    private final ArticleRepository articleRepository;

    public RssFeedService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public void fetchAndSave(String feedUrl, String sourceName) {
        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            for (SyndEntry entry : feed.getEntries()) {
                Date pubDate = entry.getPublishedDate();
                if (pubDate == null) continue;

                LocalDateTime pubDateTime = pubDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                if (pubDateTime.isBefore(LocalDateTime.now().minusHours(48))) {
                    continue;
                }

                String link = entry.getLink();
                if (articleRepository.existsByLink(link)) {
                    continue;
                }

                Article article = new Article();
                article.setTitle(entry.getTitle());
                article.setDescription(entry.getDescription() != null
                        ? entry.getDescription().getValue()
                        : null);
                article.setLink(link);
                article.setPubDate(pubDateTime);
                article.setSource(sourceName);

                articleRepository.save(article);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}