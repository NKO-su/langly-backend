package com.langly.langly_backend.scheduler;

import com.langly.langly_backend.service.RssFeedService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RssFetchScheduler {

    private final RssFeedService rssFeedService;

    private final Map<String, String> feedSources = Map.of(
            "BBC", "https://feeds.bbci.co.uk/news/rss.xml"
    );

    public RssFetchScheduler(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }

    @Scheduled(fixedRateString = "${rss.fetch.interval}")
    public void fetchAllFeeds() {
        for (Map.Entry<String, String> entry : feedSources.entrySet()) {
            String sourceName = entry.getKey();
            String feedUrl = entry.getValue();
            rssFeedService.fetchAndSave(feedUrl, sourceName);
        }
    }
}