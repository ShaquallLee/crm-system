package com.crm.dto;

import lombok.Data;

@Data
public class UserOptionVO {
    private Long id;
    private String username;
    private String realName;
    private Long roleId;
    private Long managerId;
}
