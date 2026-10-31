package com.crm.controller;

import com.crm.common.PageResult;
import com.crm.common.Result;
import com.crm.common.annotation.Audit;
import com.crm.dto.OpportunityQueryDTO;
import com.crm.entity.Opportunity;
import com.crm.service.OpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public Result<PageResult<Opportunity>> list(@Valid OpportunityQueryDTO dto) {
        return Result.success(opportunityService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    public Result<Opportunity> getById(@PathVariable Long id) {
        return Result.success(opportunityService.getAccessible(id));
    }

    @PostMapping
    @Audit(module = "商机", action = "新增商机")
    public Result<Void> create(@Valid @RequestBody Opportunity opportunity) {
        opportunityService.createOpportunity(opportunity);
        return Result.success();
    }

    @PutMapping
    @Audit(module = "商机", action = "编辑商机")
    public Result<Void> update(@Valid @RequestBody Opportunity opportunity) {
        opportunityService.updateOpportunity(opportunity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "商机", action = "删除商机")
    public Result<Void> delete(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return Result.success();
    }
}
