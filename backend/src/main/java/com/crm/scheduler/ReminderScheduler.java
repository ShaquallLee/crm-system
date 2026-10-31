package com.crm.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.entity.Activity;
import com.crm.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每天 9 点扫描逾期待办并输出告警日志；
 * 前端登录后会轮询 /activities/reminders 获取本人的实时提醒。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ActivityMapper activityMapper;

    @Scheduled(cron = "0 0 9 * * ?")
    public void scanOverdueFollowUps() {
        Long overdue = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .isNotNull(Activity::getNextFollowTime)
                .lt(Activity::getNextFollowTime, LocalDateTime.now()));
        if (overdue != null && overdue > 0) {
            log.warn("[待办提醒] 当前有 {} 条已逾期的跟进待办", overdue);
        } else {
            log.info("[待办提醒] 无逾期跟进待办");
        }
    }
}
