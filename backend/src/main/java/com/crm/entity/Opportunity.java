package com.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opportunity")
public class Opportunity extends BaseEntity {
    @NotNull(message = "请选择关联客户")
    private Long customerId;

    @NotBlank(message = "商机名称不能为空")
    @Size(max = 128, message = "商机名称最长 128 字")
    private String name;

    @Pattern(regexp = "^(LEAD|REQUIREMENT|QUOTATION|NEGOTIATION|WON|LOST)$",
            message = "商机阶段不合法")
    private String stage;

    @DecimalMin(value = "0", message = "预计金额不能为负数")
    private BigDecimal amount;

    private LocalDate expectedDate;

    @Min(value = 0, message = "赢单概率不能小于 0")
    @Max(value = 100, message = "赢单概率不能大于 100")
    private Integer probability;

    private Long ownerId;

    @Size(max = 500, message = "备注最长 500 字")
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String customerName;
}
