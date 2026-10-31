package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.crm.common.BusinessException;
import com.crm.common.ResultCode;
import com.crm.dto.LoginDTO;
import com.crm.dto.LoginVO;
import com.crm.entity.LoginLog;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.LoginLogMapper;
import com.crm.mapper.SysRoleMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.security.LoginUser;
import com.crm.security.TokenBlacklistService;
import com.crm.security.UserContext;
import com.crm.service.AuthService;
import com.crm.util.JwtUtil;
import com.crm.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_MINUTES = 15;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final LoginLogMapper loginLogMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public LoginVO login(LoginDTO dto, String ip, String userAgent) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (user == null) {
            logLogin(dto.getUsername(), null, ip, userAgent, false, "用户不存在");
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            logLogin(dto.getUsername(), user.getId(), ip, userAgent, false, "账号已禁用");
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号已禁用，请联系管理员");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, (Duration.between(now, user.getLockedUntil()).getSeconds() + 59) / 60);
            logLogin(dto.getUsername(), user.getId(), ip, userAgent, false, "账号锁定中");
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(),
                    "账号已锁定，请 " + minutes + " 分钟后重试");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            SysUser update = new SysUser();
            update.setId(user.getId());
            if (failCount >= MAX_FAIL_COUNT) {
                update.setLoginFailCount(0);
                update.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                sysUserMapper.updateById(update);
                logLogin(dto.getUsername(), user.getId(), ip, userAgent, false, "密码错误，账号锁定");
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(),
                        "密码错误次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
            }
            update.setLoginFailCount(failCount);
            sysUserMapper.updateById(update);
            logLogin(dto.getUsername(), user.getId(), ip, userAgent, false, "密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(),
                    "用户名或密码错误，还可尝试 " + (MAX_FAIL_COUNT - failCount) + " 次");
        }

        // 登录成功：清空失败计数与锁定状态
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockedUntil, null));

        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleId(), user.getTokenVersion());
        logLogin(dto.getUsername(), user.getId(), ip, userAgent, true, "登录成功");

        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(),
                user.getRoleId(), role != null ? role.getRoleName() : null);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Date expireAt = jwtUtil.getExpiration(token);
            tokenBlacklistService.revoke(token, expireAt);
        } catch (Exception e) {
            log.warn("登出时解析 Token 失败: {}", e.getMessage());
        }
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        LoginUser current = UserContext.get();
        if (current == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.selectById(current.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "原密码错误");
        }
        PasswordPolicy.validate(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        sysUserMapper.updateById(update);
    }

    private void logLogin(String username, Long userId, String ip, String userAgent,
                          boolean success, String message) {
        try {
            LoginLog item = new LoginLog();
            item.setUsername(username);
            item.setUserId(userId);
            item.setIp(ip);
            item.setUserAgent(userAgent != null && userAgent.length() > 255
                    ? userAgent.substring(0, 255) : userAgent);
            item.setSuccess(success ? 1 : 0);
            item.setMessage(message);
            loginLogMapper.insert(item);
        } catch (Exception e) {
            log.warn("写入登录日志失败: {}", e.getMessage());
        }
    }
}
