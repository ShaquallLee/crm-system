package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Long roleId;
    private String roleName;
}
