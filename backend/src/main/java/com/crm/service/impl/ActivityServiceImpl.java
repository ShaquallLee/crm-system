package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.common.BusinessException;
import com.crm.common.ResultCode;
import com.crm.dto.ReminderVO;
import com.crm.entity.Activity;
import com.crm.entity.Customer;
import com.crm.mapper.ActivityMapper;
import com.crm.mapper.CustomerMapper;
import com.crm.security.DataScopeService;
import com.crm.security.UserContext;
import com.crm.service.ActivityService;
import com.crm.service.CustomerService;
import com.crm.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final DataScopeService dataScopeService;
    private final CustomerService customerService;
    private final OpportunityService opportunityService;
    private final CustomerMapper customerMapper;

    @Override
    public List<Activity> listByCustomerId(Long customerId) {
        customerService.getAccessible(customerId);
        return list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCustomerId, customerId)
                .orderByDesc(Activity::getActivityTime));
    }

    @Override
    public List<Activity> listByOpportunityId(Long opportunityId) {
        opportunityService.getAccessible(opportunityId);
        return list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOpportunityId, opportunityId)
                .orderByDesc(Activity::getActivityTime));
    }

    @Override
    public List<Activity> listTodo() {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .isNotNull(Activity::getNextFollowTime)
                .ge(Activity::getNextFollowTime, LocalDateTime.now().minusDays(1))
                .orderByAsc(Activity::getNextFollowTime)
                .last("LIMIT 200");
        applyOperatorScope(wrapper);
        return list(wrapper);
    }

    @Override
    public ReminderVO getReminders() {
        ReminderVO vo = new ReminderVO();
        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return vo;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime upcomingEnd = now.plusDays(3);

        vo.setOverdueCount(countNextFollow(scopeIds, w -> w.lt(Activity::getNextFollowTime, now)));
        vo.setTodayCount(countNextFollow(scopeIds,
                w -> w.ge(Activity::getNextFollowTime, todayStart).lt(Activity::getNextFollowTime, todayEnd)));
        vo.setUpcomingCount(countNextFollow(scopeIds,
                w -> w.ge(Activity::getNextFollowTime, todayEnd).le(Activity::getNextFollowTime, upcomingEnd)));

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .isNotNull(Activity::getNextFollowTime)
                .le(Activity::getNextFollowTime, upcomingEnd)
                .orderByAsc(Activity::getNextFollowTime)
                .last("LIMIT 20");
        applyOperatorScope(wrapper, scopeIds);
        List<Activity> items = list(wrapper);
        fillCustomerNames(items);
        vo.setItems(items);
        return vo;
    }

    @Override
    public void createActivity(Activity activity) {
        checkParentAccess(activity.getCustomerId(), activity.getOpportunityId());
        if (activity.getCustomerId() == null && activity.getOpportunityId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请关联客户或商机");
        }
        activity.setId(null);
        activity.setCreateTime(null);
        activity.setUpdateTime(null);
        activity.setDeleted(null);
        activity.setOperatorId(UserContext.getUserId());
        if (activity.getActivityTime() == null) {
            activity.setActivityTime(LocalDateTime.now());
        }
        save(activity);
    }

    @Override
    public void updateActivity(Activity activity) {
        Activity old = requireActivity(activity.getId());
        checkOperatorAccess(old.getOperatorId());
        checkParentAccess(activity.getCustomerId(), activity.getOpportunityId());
        if (activity.getCustomerId() == null && activity.getOpportunityId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请关联客户或商机");
        }
        activity.setOperatorId(old.getOperatorId());
        activity.setCreateTime(null);
        activity.setUpdateTime(null);
        activity.setDeleted(null);
        updateById(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        Activity old = requireActivity(id);
        checkOperatorAccess(old.getOperatorId());
        removeById(id);
    }

    private Activity requireActivity(Long id) {
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "跟进记录不存在");
        }
        return activity;
    }

    private void checkParentAccess(Long customerId, Long opportunityId) {
        if (customerId != null) {
            customerService.getAccessible(customerId);
        }
        if (opportunityId != null) {
            opportunityService.getAccessible(opportunityId);
        }
    }

    private void checkOperatorAccess(Long operatorId) {
        if (operatorId != null) {
            dataScopeService.checkUserAccess(operatorId);
        }
    }

    private void applyOperatorScope(LambdaQueryWrapper<Activity> wrapper) {
        applyOperatorScope(wrapper, dataScopeService.accessibleUserIds());
    }

    private void applyOperatorScope(LambdaQueryWrapper<Activity> wrapper, List<Long> scopeIds) {
        if (scopeIds != null) {
            wrapper.in(Activity::getOperatorId, scopeIds);
        }
    }

    private int countNextFollow(List<Long> scopeIds,
                                java.util.function.Consumer<LambdaQueryWrapper<Activity>> condition) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>().isNotNull(Activity::getNextFollowTime);
        condition.accept(wrapper);
        applyOperatorScope(wrapper, scopeIds);
        Long count = count(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private void fillCustomerNames(List<Activity> items) {
        if (items.isEmpty()) {
            return;
        }
        Set<Long> customerIds = items.stream().map(Activity::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (customerIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        items.forEach(a -> a.setCustomerName(nameMap.getOrDefault(a.getCustomerId(), "")));
    }
}
