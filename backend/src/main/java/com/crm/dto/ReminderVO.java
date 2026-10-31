package com.crm.dto;

import com.crm.entity.Activity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReminderVO {
    private int overdueCount;
    private int todayCount;
    private int upcomingCount;
    private List<Activity> items = new ArrayList<>();
}
