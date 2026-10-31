package com.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.common.PageResult;
import com.crm.dto.OpportunityQueryDTO;
import com.crm.entity.Opportunity;

public interface OpportunityService extends IService<Opportunity> {
    PageResult<Opportunity> pageQuery(OpportunityQueryDTO dto);

    Opportunity getAccessible(Long id);

    void createOpportunity(Opportunity opportunity);

    void updateOpportunity(Opportunity opportunity);

    void deleteOpportunity(Long id);
}
