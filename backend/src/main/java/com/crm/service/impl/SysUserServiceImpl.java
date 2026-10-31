package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.common.BusinessException;
import com.crm.common.PageResult;
import com.crm.common.ResultCode;
import com.crm.dto.UserOptionVO;
import com.crm.dto.UserQueryDTO;
import com.crm.dto.UserVO;
import com.crm.entity.Customer;
import com.crm.entity.Opportunity;
import com.crm.entity.SysRole;
import com.crm.entity.SysUser;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.OpportunityMapper;
import com.crm.mapper.SysRoleMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.security.DataScopeService;
import com.crm.security.UserContext;
import com.crm.service.SysUserService;
import com.crm.util.PageUtil;
import com.crm.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleMapper sysRoleMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DataScopeService dataScopeService;

    @Override
    public PageResult<UserVO> pageQuery(UserQueryDTO dto) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            wrapper.like(SysUser::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getRealName())) {
            wrapper.like(SysUser::getRealName, dto.getRealName());
        }
        if (dto.getRoleId() != null) {
            wrapper.eq(SysUser::getRoleId, dto.getRoleId());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = page(PageUtil.of(dto.getPage(), dto.getSize()), wrapper);

        List<Long> roleIds = result.getRecords().stream()
                .map(SysUser::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> roleNameMap = roleIds.isEmpty() ? Map.of()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));

        List<Long> managerIds = result.getRecords().stream()
                .map(SysUser::getManagerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> managerNameMap = managerIds.isEmpty() ? Map.of()
                : listByIds(managerIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        List<UserVO> voList = result.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setRoleId(u.getRoleId());
            vo.setRoleName(roleNameMap.get(u.getRoleId()));
            vo.setManagerId(u.getManagerId());
            vo.setManagerName(u.getManagerId() == null ? ""
                    : managerNameMap.getOrDefault(u.getManagerId(), ""));
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        PageResult<UserVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setRecords(voList);
        return pageResult;
    }

    @Override
    public List<UserOptionVO> listOptions() {
        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName,
                        SysUser::getRoleId, SysUser::getManagerId)
                .eq(SysUser::getStatus, 1);
        if (scopeIds != null) {
            wrapper.in(SysUser::getId, scopeIds);
        }
        wrapper.orderByAsc(SysUser::getRoleId).orderByAsc(SysUser::getId);
        return list(wrapper).stream().map(u -> {
            UserOptionVO vo = new UserOptionVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setRoleId(u.getRoleId());
            vo.setManagerId(u.getManagerId());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(SysUser user) {
        user.setId(null);
        user.setCreateTime(null);
        user.setUpdateTime(null);
        user.setDeleted(null);
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setTokenVersion(0);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setUsername(user.getUsername().trim());
        checkUsernameUnique(user.getUsername(), null);
        validateRole(user.getRoleId());
        validateManager(null, user.getManagerId());
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请设置初始密码");
        }
        PasswordPolicy.validate(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户 ID 不能为空");
        }
        SysUser old = getById(user.getId());
        if (old == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (StringUtils.hasText(user.getUsername())) {
            user.setUsername(user.getUsername().trim());
            if (!user.getUsername().equals(old.getUsername())) {
                checkUsernameUnique(user.getUsername(), user.getId());
            }
        } else {
            user.setUsername(old.getUsername());
        }
        validateRole(user.getRoleId());
        validateManager(user.getId(), user.getManagerId());

        user.setPassword(null);
        user.setCreateTime(null);
        user.setUpdateTime(null);
        user.setDeleted(null);
        user.setLoginFailCount(null);
        user.setLockedUntil(null);
        user.setTokenVersion(null);
        if (user.getStatus() == null) {
            user.setStatus(old.getStatus());
        }
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (Objects.equals(userId, UserContext.getUserId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能删除当前登录账号");
        }
        Long subordinates = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getManagerId, userId));
        if (subordinates != null && subordinates > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该用户仍管理着 " + subordinates + " 名下属，请先调整下属的直属上级");
        }
        Long customers = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getOwnerId, userId));
        if (customers != null && customers > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该用户仍负责 " + customers + " 个客户，请先转移客户负责人");
        }
        Long opportunities = opportunityMapper.selectCount(
                new LambdaQueryWrapper<Opportunity>().eq(Opportunity::getOwnerId, userId));
        if (opportunities != null && opportunities > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该用户仍负责 " + opportunities + " 个商机，请先转移商机负责人");
        }
        if (user.getRoleId() != null && user.getRoleId() == UserContext.ROLE_ADMIN) {
            Long admins = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, UserContext.ROLE_ADMIN));
            if (admins != null && admins <= 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "系统至少需要保留一个管理员");
            }
        }

        // 逻辑删除后释放用户名唯一约束
        String base = user.getUsername().length() > 50
                ? user.getUsername().substring(0, 50) : user.getUsername();
        SysUser rename = new SysUser();
        rename.setId(userId);
        rename.setUsername(base + "#deleted#" + userId);
        updateById(rename);
        removeById(userId);
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        PasswordPolicy.validate(newPassword);
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword))
                .setSql("token_version = token_version + 1"));
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        Long count = count(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }
    }

    private void validateRole(Long roleId) {
        if (roleId == null || sysRoleMapper.selectById(roleId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "角色不存在");
        }
    }

    private void validateManager(Long userId, Long managerId) {
        if (managerId == null) {
            return;
        }
        if (userId != null && managerId.equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "直属上级不能是本人");
        }
        SysUser manager = getById(managerId);
        if (manager == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "直属上级不存在");
        }
        if (manager.getRoleId() == null || manager.getRoleId() == UserContext.ROLE_SALES) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "直属上级必须是管理员或销售经理");
        }
        Long cursor = managerId;
        int guard = 0;
        while (cursor != null && guard++ < 20) {
            if (userId != null && cursor.equals(userId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "直属上级不能形成循环");
            }
            SysUser current = getById(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getManagerId();
        }
    }
}
