package com.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.BusinessException;
import com.crm.common.PageResult;
import com.crm.common.Result;
import com.crm.common.ResultCode;
import com.crm.dto.AuditLogQueryDTO;
import com.crm.dto.LoginLogQueryDTO;
import com.crm.entity.AuditLog;
import com.crm.entity.LoginLog;
import com.crm.mapper.AuditLogMapper;
import com.crm.mapper.LoginLogMapper;
import com.crm.security.UserContext;
import com.crm.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogMapper auditLogMapper;
    private final LoginLogMapper loginLogMapper;

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @GetMapping("/operations")
    public Result<PageResult<AuditLog>> operations(@Valid AuditLogQueryDTO dto) {
        checkAdmin();
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            wrapper.like(AuditLog::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getModule())) {
            wrapper.eq(AuditLog::getModule, dto.getModule());
        }
        if (StringUtils.hasText(dto.getAction())) {
            wrapper.eq(AuditLog::getAction, dto.getAction());
        }
        if (dto.getSuccess() != null) {
            wrapper.eq(AuditLog::getSuccess, dto.getSuccess());
        }
        if (dto.getStartDate() != null) {
            wrapper.ge(AuditLog::getCreateTime, dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            wrapper.lt(AuditLog::getCreateTime, dto.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);

        Page<AuditLog> page = auditLogMapper.selectPage(PageUtil.of(dto.getPage(), dto.getSize()), wrapper);
        return Result.success(new PageResult<>(page));
    }

    @GetMapping("/logins")
    public Result<PageResult<LoginLog>> logins(@Valid LoginLogQueryDTO dto) {
        checkAdmin();
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            wrapper.like(LoginLog::getUsername, dto.getUsername());
        }
        if (dto.getSuccess() != null) {
            wrapper.eq(LoginLog::getSuccess, dto.getSuccess());
        }
        if (dto.getStartDate() != null) {
            wrapper.ge(LoginLog::getCreateTime, dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            wrapper.lt(LoginLog::getCreateTime, dto.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(LoginLog::getCreateTime);

        Page<LoginLog> page = loginLogMapper.selectPage(PageUtil.of(dto.getPage(), dto.getSize()), wrapper);
        return Result.success(new PageResult<>(page));
    }
}
