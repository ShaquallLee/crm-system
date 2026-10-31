package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("opportunity_stage_log")
public class OpportunityStageLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long opportunityId;
    private String fromStage;
    private String toStage;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
