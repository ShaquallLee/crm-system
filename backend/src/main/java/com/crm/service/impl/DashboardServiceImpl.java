package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.crm.dto.DashboardStatsVO;
import com.crm.entity.Activity;
import com.crm.entity.Customer;
import com.crm.entity.Opportunity;
import com.crm.entity.SysUser;
import com.crm.mapper.ActivityMapper;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.OpportunityMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.security.DataScopeService;
import com.crm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final ActivityMapper activityMapper;
    private final SysUserMapper sysUserMapper;
    private final DataScopeService dataScopeService;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setCustomerTotal(0L);
        vo.setCustomerNewThisMonth(0L);
        vo.setOpportunityTotal(0L);
        vo.setAmountThisMonth(BigDecimal.ZERO);
        vo.setStageDistribution(List.of());
        vo.setOwnerRanking(List.of());
        vo.setActivityTrend(List.of());

        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return vo;
        }

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        vo.setCustomerTotal(customerMapper.selectCount(scopeCustomer(scopeIds)));
        vo.setCustomerNewThisMonth(customerMapper.selectCount(
                scopeCustomer(scopeIds).ge(Customer::getCreateTime, monthStart)));
        vo.setOpportunityTotal(opportunityMapper.selectCount(scopeOpportunity(scopeIds)));

        List<Opportunity> monthOpps = opportunityMapper.selectList(
                scopeOpportunity(scopeIds)
                        .ne(Opportunity::getStage, "LOST")
                        .ge(Opportunity::getExpectedDate, monthStart.toLocalDate())
                        .lt(Opportunity::getExpectedDate, monthEnd.toLocalDate()));
        BigDecimal amount = monthOpps.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setAmountThisMonth(amount);

        QueryWrapper<Opportunity> stageWrapper = new QueryWrapper<Opportunity>()
                .select("stage", "count(*) as count")
                .groupBy("stage");
        if (scopeIds != null) {
            stageWrapper.in("owner_id", scopeIds);
        }
        vo.setStageDistribution(opportunityMapper.selectMaps(stageWrapper));

        QueryWrapper<Opportunity> rankingWrapper = new QueryWrapper<Opportunity>()
                .select("owner_id", "sum(amount) as total_amount", "count(*) as opp_count")
                .groupBy("owner_id")
                .orderByDesc("total_amount");
        if (scopeIds != null) {
            rankingWrapper.in("owner_id", scopeIds);
        }
        List<Map<String, Object>> ranking = opportunityMapper.selectMaps(rankingWrapper);
        fillOwnerNames(ranking);
        vo.setOwnerRanking(ranking);

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            LambdaQueryWrapper<Activity> activityWrapper = new LambdaQueryWrapper<Activity>()
                    .ge(Activity::getActivityTime, start)
                    .lt(Activity::getActivityTime, end);
            if (scopeIds != null) {
                activityWrapper.in(Activity::getOperatorId, scopeIds);
            }
            Long count = activityMapper.selectCount(activityWrapper);
            Map<String, Object> item = new HashMap<>();
            item.put("date", day.format(fmt));
            item.put("count", count);
            trend.add(item);
        }
        vo.setActivityTrend(trend);

        return vo;
    }

    private LambdaQueryWrapper<Customer> scopeCustomer(List<Long> scopeIds) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (scopeIds != null) {
            wrapper.in(Customer::getOwnerId, scopeIds);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Opportunity> scopeOpportunity(List<Long> scopeIds) {
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        if (scopeIds != null) {
            wrapper.in(Opportunity::getOwnerId, scopeIds);
        }
        return wrapper;
    }

    private void fillOwnerNames(List<Map<String, Object>> ranking) {
        if (ranking.isEmpty()) {
            return;
        }
        List<Long> ownerIds = ranking.stream()
                .map(m -> m.get("owner_id"))
                .filter(Objects::nonNull)
                .map(v -> ((Number) v).longValue())
                .collect(Collectors.toList());
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        ranking.forEach(m -> {
            Object id = m.get("owner_id");
            if (id != null) {
                m.put("ownerName", nameMap.getOrDefault(((Number) id).longValue(), "未知"));
            }
        });
    }
}
