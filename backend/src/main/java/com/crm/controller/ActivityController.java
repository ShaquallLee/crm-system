package com.crm.controller;

import com.crm.common.Result;
import com.crm.common.annotation.Audit;
import com.crm.dto.ReminderVO;
import com.crm.entity.Activity;
import com.crm.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public Result<List<Activity>> listTodo() {
        return Result.success(activityService.listTodo());
    }

    @GetMapping("/todo")
    public Result<List<Activity>> todo() {
        return Result.success(activityService.listTodo());
    }

    @GetMapping("/reminders")
    public Result<ReminderVO> reminders() {
        return Result.success(activityService.getReminders());
    }

    @GetMapping("/customer/{customerId}")
    public Result<List<Activity>> listByCustomer(@PathVariable Long customerId) {
        return Result.success(activityService.listByCustomerId(customerId));
    }

    @GetMapping("/opportunity/{opportunityId}")
    public Result<List<Activity>> listByOpportunity(@PathVariable Long opportunityId) {
        return Result.success(activityService.listByOpportunityId(opportunityId));
    }

    @PostMapping
    @Audit(module = "跟进", action = "新增跟进记录")
    public Result<Void> create(@Valid @RequestBody Activity activity) {
        activityService.createActivity(activity);
        return Result.success();
    }

    @PutMapping
    @Audit(module = "跟进", action = "编辑跟进记录")
    public Result<Void> update(@Valid @RequestBody Activity activity) {
        activityService.updateActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "跟进", action = "删除跟进记录")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return Result.success();
    }
}
