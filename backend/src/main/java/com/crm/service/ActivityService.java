package com.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.dto.ReminderVO;
import com.crm.entity.Activity;

import java.util.List;

public interface ActivityService extends IService<Activity> {
    List<Activity> listByCustomerId(Long customerId);

    List<Activity> listByOpportunityId(Long opportunityId);

    List<Activity> listTodo();

    ReminderVO getReminders();

    void createActivity(Activity activity);

    void updateActivity(Activity activity);

    void deleteActivity(Long id);
}
