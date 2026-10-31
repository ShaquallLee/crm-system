package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.common.BusinessException;
import com.crm.common.PageResult;
import com.crm.common.ResultCode;
import com.crm.dto.OpportunityQueryDTO;
import com.crm.entity.Customer;
import com.crm.entity.Opportunity;
import com.crm.entity.OpportunityStageLog;
import com.crm.entity.SysUser;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.OpportunityMapper;
import com.crm.mapper.OpportunityStageLogMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.security.DataScopeService;
import com.crm.security.UserContext;
import com.crm.service.CustomerService;
import com.crm.service.OpportunityService;
import com.crm.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpportunityServiceImpl extends ServiceImpl<OpportunityMapper, Opportunity> implements OpportunityService {

    private static final Set<String> STAGES =
            Set.of("LEAD", "REQUIREMENT", "QUOTATION", "NEGOTIATION", "WON", "LOST");

    private final OpportunityStageLogMapper stageLogMapper;
    private final DataScopeService dataScopeService;
    private final CustomerService customerService;
    private final SysUserMapper sysUserMapper;
    private final CustomerMapper customerMapper;

    @Override
    public PageResult<Opportunity> pageQuery(OpportunityQueryDTO dto) {
        Page<Opportunity> page = PageUtil.of(dto.getPage(), dto.getSize());
        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return new PageResult<>(new Page<>(page.getCurrent(), page.getSize(), 0));
        }
        Page<Opportunity> result = page(page, buildWrapper(dto, scopeIds));
        fillNames(result.getRecords());
        return new PageResult<>(result);
    }

    @Override
    public Opportunity getAccessible(Long id) {
        Opportunity opportunity = getById(id);
        if (opportunity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商机不存在");
        }
        dataScopeService.checkUserAccess(opportunity.getOwnerId());
        return opportunity;
    }

    @Override
    public void createOpportunity(Opportunity opportunity) {
        customerService.getAccessible(opportunity.getCustomerId());
        opportunity.setId(null);
        opportunity.setCreateTime(null);
        opportunity.setUpdateTime(null);
        opportunity.setDeleted(null);
        String stage = StringUtils.hasText(opportunity.getStage()) ? opportunity.getStage() : "LEAD";
        if (!STAGES.contains(stage)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商机阶段不合法");
        }
        opportunity.setStage(stage);
        if (opportunity.getAmount() == null) {
            opportunity.setAmount(BigDecimal.ZERO);
        }
        if (opportunity.getProbability() == null) {
            opportunity.setProbability(0);
        }
        opportunity.setOwnerId(dataScopeService.resolveOwnerForCreate(opportunity.getOwnerId()));
        save(opportunity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunity(Opportunity opportunity) {
        if (opportunity.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商机 ID 不能为空");
        }
        Opportunity old = getAccessible(opportunity.getId());
        customerService.getAccessible(opportunity.getCustomerId());

        String stage = StringUtils.hasText(opportunity.getStage()) ? opportunity.getStage() : old.getStage();
        if (!STAGES.contains(stage)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商机阶段不合法");
        }
        opportunity.setStage(stage);
        opportunity.setOwnerId(dataScopeService.resolveOwnerForUpdate(old.getOwnerId(), opportunity.getOwnerId()));
        opportunity.setCreateTime(null);
        opportunity.setUpdateTime(null);
        opportunity.setDeleted(null);

        boolean updated = updateById(opportunity);
        if (updated && old.getStage() != null && !old.getStage().equals(stage)) {
            OpportunityStageLog log = new OpportunityStageLog();
            log.setOpportunityId(opportunity.getId());
            log.setFromStage(old.getStage());
            log.setToStage(stage);
            log.setOperatorId(UserContext.getUserId());
            stageLogMapper.insert(log);
        }
    }

    @Override
    public void deleteOpportunity(Long id) {
        getAccessible(id);
        removeById(id);
    }

    private LambdaQueryWrapper<Opportunity> buildWrapper(OpportunityQueryDTO dto, List<Long> scopeIds) {
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getName())) {
            wrapper.like(Opportunity::getName, dto.getName());
        }
        if (StringUtils.hasText(dto.getStage())) {
            wrapper.eq(Opportunity::getStage, dto.getStage());
        }
        if (dto.getCustomerId() != null) {
            wrapper.eq(Opportunity::getCustomerId, dto.getCustomerId());
        }
        if (dto.getOwnerId() != null) {
            wrapper.eq(Opportunity::getOwnerId, dto.getOwnerId());
        }
        if (scopeIds != null) {
            wrapper.in(Opportunity::getOwnerId, scopeIds);
        }
        wrapper.orderByDesc(Opportunity::getCreateTime);
        return wrapper;
    }

    private void fillNames(List<Opportunity> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> ownerIds = records.stream().map(Opportunity::getOwnerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!ownerIds.isEmpty()) {
            Map<Long, String> ownerMap = sysUserMapper.selectBatchIds(ownerIds).stream()
                    .collect(Collectors.toMap(SysUser::getId,
                            u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            records.forEach(o -> o.setOwnerName(ownerMap.getOrDefault(o.getOwnerId(), "")));
        }
        Set<Long> customerIds = records.stream().map(Opportunity::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!customerIds.isEmpty()) {
            Map<Long, String> customerMap = customerMapper.selectBatchIds(customerIds).stream()
                    .collect(Collectors.toMap(Customer::getId, Customer::getName));
            records.forEach(o -> o.setCustomerName(customerMap.getOrDefault(o.getCustomerId(), "")));
        }
    }
}
