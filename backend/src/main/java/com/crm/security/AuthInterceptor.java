package com.crm.security;

import com.crm.common.BusinessException;
import com.crm.common.ResultCode;
import com.crm.entity.SysUser;
import com.crm.mapper.SysUserMapper;
import com.crm.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器：校验签名/过期时间、黑名单、用户状态与 Token 版本
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(header);
        if (authHeader == null || !authHeader.startsWith(prefix)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(prefix.length());
        if (jwtUtil.isTokenExpired(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (tokenBlacklistService.isRevoked(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Claims claims = jwtUtil.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号不存在或已禁用");
        }

        int tokenVersion = jwtUtil.getTokenVersion(token);
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (tokenVersion != currentVersion) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效，请重新登录");
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getRoleId(),
                user.getManagerId(),
                user.getRealName()
        );
        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
