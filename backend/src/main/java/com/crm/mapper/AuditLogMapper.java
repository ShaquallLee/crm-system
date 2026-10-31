package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
