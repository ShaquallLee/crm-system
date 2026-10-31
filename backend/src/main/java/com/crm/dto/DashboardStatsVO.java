package com.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsVO {
    private Long customerTotal;
    private Long customerNewThisMonth;
    private Long opportunityTotal;
    private BigDecimal amountThisMonth;
    private List<Map<String, Object>> stageDistribution;
    private List<Map<String, Object>> ownerRanking;
    private List<Map<String, Object>> activityTrend;
}
