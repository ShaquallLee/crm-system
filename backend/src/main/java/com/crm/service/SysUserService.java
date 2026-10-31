package com.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.common.PageResult;
import com.crm.dto.UserOptionVO;
import com.crm.dto.UserQueryDTO;
import com.crm.dto.UserVO;
import com.crm.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    PageResult<UserVO> pageQuery(UserQueryDTO dto);

    List<UserOptionVO> listOptions();

    void createUser(SysUser user);

    void updateUser(SysUser user);

    void deleteUser(Long userId);

    boolean resetPassword(Long userId, String newPassword);
}
