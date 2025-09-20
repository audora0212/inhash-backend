package com.inhash.backend.scheduler;

import com.inhash.backend.service.CrawlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private final CrawlService crawlService;

    @Value("${inhash.scheduler.enabled:true}")
    private boolean enabled;

    public CrawlScheduler(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Scheduled(cron = "${inhash.scheduler.cron}")
    public void scheduledCrawl() {
        if (!enabled) return;
        crawlService.runCrawlAndImport();
    }
}


