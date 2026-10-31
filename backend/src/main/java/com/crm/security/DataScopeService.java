package com.crm.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.BusinessException;
import com.crm.common.ResultCode;
import com.crm.entity.SysUser;
import com.crm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限：
 * 管理员 → 全部；销售经理 → 自己及全部下级；销售代表 → 仅自己
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final SysUserMapper sysUserMapper;

    /**
     * @return null 表示不限制（管理员），否则为可访问的用户 ID 集合
     */
    public List<Long> accessibleUserIds() {
        LoginUser current = UserContext.get();
        if (current == null) {
            return List.of();
        }
        if (current.getRoleId() != null && current.getRoleId() == UserContext.ROLE_ADMIN) {
            return null;
        }
        if (current.getRoleId() == null || current.getRoleId() != UserContext.ROLE_MANAGER) {
            return List.of(current.getUserId());
        }

        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getManagerId));
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysUser user : users) {
            if (user.getManagerId() != null) {
                children.computeIfAbsent(user.getManagerId(), k -> new ArrayList<>()).add(user.getId());
            }
        }
        Set<Long> ids = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(current.getUserId());
        ids.add(current.getUserId());
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            for (Long child : children.getOrDefault(id, List.of())) {
                if (ids.add(child)) {
                    queue.add(child);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    public boolean isAllData() {
        return accessibleUserIds() == null;
    }

    public boolean canAccessUser(Long userId) {
        if (userId == null) {
            return isAllData();
        }
        List<Long> ids = accessibleUserIds();
        return ids == null || ids.contains(userId);
    }

    public void checkUserAccess(Long userId) {
        if (!canAccessUser(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 新增时确定负责人：销售强制为本人；经理只能分配给自己及下级；管理员可指定
     */
    public Long resolveOwnerForCreate(Long requestedOwnerId) {
        LoginUser current = UserContext.get();
        if (current == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (current.getRoleId() != null && current.getRoleId() == UserContext.ROLE_SALES) {
            return current.getUserId();
        }
        if (requestedOwnerId == null) {
            return current.getUserId();
        }
        if (!canAccessUser(requestedOwnerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能分配给本人或直属下级");
        }
        return requestedOwnerId;
    }

    /**
     * 编辑时校验原负责人越权，并确定新负责人
     */
    public Long resolveOwnerForUpdate(Long currentOwnerId, Long requestedOwnerId) {
        checkUserAccess(currentOwnerId);
        LoginUser current = UserContext.get();
        if (current == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (current.getRoleId() != null && current.getRoleId() == UserContext.ROLE_SALES) {
            return currentOwnerId;
        }
        if (requestedOwnerId == null) {
            return currentOwnerId;
        }
        if (!canAccessUser(requestedOwnerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能分配给本人或直属下级");
        }
        return requestedOwnerId;
    }
}
