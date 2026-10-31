package com.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称最长 128 字")
    private String name;

    @Size(max = 64, message = "行业最长 64 字")
    private String industry;

    @Pattern(regexp = "^[ABCD]$", message = "客户级别只能是 A/B/C/D")
    private String level;

    @Size(max = 64, message = "客户来源最长 64 字")
    private String source;

    private Long ownerId;

    @Size(max = 255, message = "地址最长 255 字")
    private String address;

    @Size(max = 500, message = "备注最长 500 字")
    private String remark;

    @TableField(exist = false)
    private String ownerName;
}
