package com.crm.controller;

import com.crm.common.BusinessException;
import com.crm.common.PageResult;
import com.crm.common.Result;
import com.crm.common.ResultCode;
import com.crm.common.annotation.Audit;
import com.crm.dto.ResetPasswordDTO;
import com.crm.dto.UserOptionVO;
import com.crm.dto.UserQueryDTO;
import com.crm.dto.UserVO;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.SysRoleMapper;
import com.crm.security.UserContext;
import com.crm.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @GetMapping
    public Result<PageResult<UserVO>> list(@Valid UserQueryDTO dto) {
        checkAdmin();
        return Result.success(sysUserService.pageQuery(dto));
    }

    /**
     * 负责人下拉选项：管理员全部、经理本团队、销售仅本人
     */
    @GetMapping("/options")
    public Result<List<UserOptionVO>> options() {
        return Result.success(sysUserService.listOptions());
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        if (!UserContext.isAdmin() && !Objects.equals(id, UserContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> listRoles() {
        checkAdmin();
        return Result.success(sysRoleMapper.selectList(null));
    }

    @PostMapping
    @Audit(module = "用户", action = "新增用户")
    public Result<Void> create(@Valid @RequestBody SysUser user) {
        checkAdmin();
        sysUserService.createUser(user);
        return Result.success();
    }

    @PutMapping
    @Audit(module = "用户", action = "编辑用户")
    public Result<Void> update(@Valid @RequestBody SysUser user) {
        checkAdmin();
        if (Objects.equals(user.getId(), UserContext.getUserId())) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能禁用当前登录账号");
            }
            if (user.getRoleId() != null && user.getRoleId() != UserContext.ROLE_ADMIN) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能修改自己的角色");
            }
        }
        sysUserService.updateUser(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "用户", action = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @Audit(module = "用户", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO body) {
        checkAdmin();
        sysUserService.resetPassword(id, body.getPassword());
        return Result.success();
    }
}
