package com.inhash.backend.scheduler;

import com.inhash.backend.service.CrawlService;
import com.inhash.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private final CrawlService crawlService;
    private final NotificationService notificationService;

    @Value("${inhash.scheduler.enabled:true}")
    private boolean enabled;
    
    @Value("${inhash.scheduler.crawl.enabled:false}")
    private boolean crawlEnabled; // 서버 크롤링 비활성화 (클라이언트 크롤링으로 전환)

    public CrawlScheduler(CrawlService crawlService, NotificationService notificationService) {
        this.crawlService = crawlService;
        this.notificationService = notificationService;
    }

    /**
     * 기존 서버 크롤링 (비활성화 예정)
     * @deprecated 클라이언트 크롤링으로 전환
     */
    @Scheduled(cron = "${inhash.scheduler.cron}")
    @Deprecated
    public void scheduledCrawl() {
        if (!enabled || !crawlEnabled) return;
        crawlService.runCrawlAndImport();
    }
    
    /**
     * 매일 아침 9시 - 과제/수업 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyNotifications() {
        if (!enabled) return;
        notificationService.sendDailyReminders();
    }
    
    /**
     * 매일 아침 9시 - 미업데이트 사용자 알림
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendUpdateReminders() {
        if (!enabled) return;
        notificationService.sendUpdateReminders();
    }
}


