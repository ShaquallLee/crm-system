package com.crm.service;

import com.crm.dto.LoginDTO;
import com.crm.dto.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO dto, String ip, String userAgent);

    void logout(String token);

    void changePassword(String oldPassword, String newPassword);
}
