package com.crm.controller;

import com.crm.common.Result;
import com.crm.common.annotation.Audit;
import com.crm.dto.ChangePasswordDTO;
import com.crm.dto.LoginDTO;
import com.crm.dto.LoginVO;
import com.crm.security.LoginUser;
import com.crm.security.UserContext;
import com.crm.service.AuthService;
import com.crm.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.prefix}")
    private String prefix;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return Result.success(authService.login(dto, ip, userAgent));
    }

    @GetMapping("/userinfo")
    public Result<LoginUser> userinfo() {
        return Result.success(UserContext.get());
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith(prefix)) {
            authService.logout(authHeader.substring(prefix.length()));
        }
        UserContext.clear();
        return Result.success();
    }

    @Audit(module = "认证", action = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
