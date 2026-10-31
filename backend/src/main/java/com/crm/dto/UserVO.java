package com.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long roleId;
    private String roleName;
    private Long managerId;
    private String managerName;
    private Integer status;
    private LocalDateTime createTime;
}
