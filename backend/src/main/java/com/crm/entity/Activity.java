package com.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class Activity extends BaseEntity {
    private Long customerId;
    private Long opportunityId;

    @NotBlank(message = "跟进类型不能为空")
    @Pattern(regexp = "^(CALL|MEETING|EMAIL|VISIT|OTHER)$", message = "跟进类型不合法")
    private String type;

    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 1000, message = "跟进内容最长 1000 字")
    private String content;

    private LocalDateTime activityTime;
    private LocalDateTime nextFollowTime;
    private Long operatorId;

    @TableField(exist = false)
    private String customerName;
}
