package com.inhash.backend.service;

import com.inhash.backend.domain.FcmToken;
import com.inhash.backend.domain.StudentUpdateStatus;
import com.inhash.backend.repository.FcmTokenRepository;
import com.inhash.backend.repository.StudentUpdateStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * FCM 푸시 알림 서비스
 * - 매일 아침 9시 과제/수업 알림
 * - 미업데이트 사용자 알림
 */
@Service
public class NotificationService {
    
    private final StudentUpdateStatusRepository updateStatusRepository;
    private final FcmTokenRepository fcmTokenRepository;
    
    @Value("${inhash.notification.update-reminder.days:2,4,7}")
    private String updateReminderDays;
    
    public NotificationService(
            StudentUpdateStatusRepository updateStatusRepository,
            FcmTokenRepository fcmTokenRepository) {
        this.updateStatusRepository = updateStatusRepository;
        this.fcmTokenRepository = fcmTokenRepository;
    }
    
    /**
     * 미업데이트 사용자에게 알림 발송
     * 2일, 4일, 7일 단위로 알림
     */
    @Transactional
    public void sendUpdateReminders() {
        Instant now = Instant.now();
        String[] days = updateReminderDays.split(",");
        
        for (String dayStr : days) {
            int day = Integer.parseInt(dayStr.trim());
            Instant targetDate = now.minus(Duration.ofDays(day));
            Instant lastNotificationBefore = now.minus(Duration.ofDays(1)); // 하루에 한 번만
            
            List<StudentUpdateStatus> targets = updateStatusRepository
                    .findNotificationTargets(targetDate, lastNotificationBefore);
            
            for (StudentUpdateStatus status : targets) {
                // 알림 발송 일수에 따라 다른 메시지
                String message = getUpdateReminderMessage(day, status.getNotificationCount());
                
                // FCM 토큰 조회
                List<FcmToken> tokens = fcmTokenRepository.findByStudentId(status.getStudent().getId());
                
                for (FcmToken token : tokens) {
                    sendFcmNotification(token.getToken(), "INHASH 업데이트 필요", message);
                }
                
                // 알림 발송 기록 업데이트
                status.setLastNotificationSentAt(now);
                status.setNotificationCount(status.getNotificationCount() + 1);
                updateStatusRepository.save(status);
            }
        }
    }
    
    /**
     * 매일 아침 9시 과제/수업 알림
     */
    public void sendDailyReminders() {
        // TODO: 각 학생의 과제/수업 정보를 조회하여 알림 발송
        // 이 부분은 기존 로직 활용
    }
    
    private String getUpdateReminderMessage(int days, int count) {
        if (days <= 2) {
            return "LMS 데이터가 2일간 업데이트되지 않았습니다. 앱을 실행하여 최신 정보를 확인해주세요.";
        } else if (days <= 4) {
            return "LMS 데이터가 4일간 업데이트되지 않았습니다. 과제를 놓치지 않으려면 앱을 실행해주세요!";
        } else {
            return "LMS 데이터가 일주일간 업데이트되지 않았습니다. 지금 바로 확인해보세요!";
        }
    }
    
    private void sendFcmNotification(String token, String title, String body) {
        // TODO: FCM SDK를 사용하여 실제 푸시 알림 발송
        // Firebase Admin SDK 설정 필요
        System.out.println("Sending FCM: token=" + token + ", title=" + title + ", body=" + body);
    }
}


