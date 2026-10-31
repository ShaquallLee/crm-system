package com.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact")
public class Contact extends BaseEntity {
    @NotNull(message = "请选择关联客户")
    private Long customerId;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名最长 64 字")
    private String name;

    @Size(max = 64, message = "职位最长 64 字")
    private String position;

    @Size(max = 32, message = "电话最长 32 字")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱最长 128 字")
    private String email;

    @Min(value = 0, message = "主联系人标记不合法")
    @Max(value = 1, message = "主联系人标记不合法")
    private Integer isPrimary;

    @Size(max = 255, message = "备注最长 255 字")
    private String remark;
}
