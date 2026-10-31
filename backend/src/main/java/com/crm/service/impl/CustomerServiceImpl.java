package com.crm.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.common.BusinessException;
import com.crm.common.PageResult;
import com.crm.common.ResultCode;
import com.crm.dto.CustomerExcelRow;
import com.crm.dto.CustomerQueryDTO;
import com.crm.dto.ExcelImportResult;
import com.crm.entity.Customer;
import com.crm.entity.SysUser;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.security.DataScopeService;
import com.crm.security.UserContext;
import com.crm.service.CustomerService;
import com.crm.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_EXPORT_ROWS = 10000;

    private final DataScopeService dataScopeService;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<Customer> pageQuery(CustomerQueryDTO dto) {
        Page<Customer> page = PageUtil.of(dto.getPage(), dto.getSize());
        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return new PageResult<>(new Page<>(page.getCurrent(), page.getSize(), 0));
        }
        Page<Customer> result = page(page, buildWrapper(dto, scopeIds));
        fillOwnerName(result.getRecords());
        return new PageResult<>(result);
    }

    @Override
    public Customer getAccessible(Long id) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "客户不存在");
        }
        dataScopeService.checkUserAccess(customer.getOwnerId());
        return customer;
    }

    @Override
    public void createCustomer(Customer customer) {
        customer.setId(null);
        customer.setCreateTime(null);
        customer.setUpdateTime(null);
        customer.setDeleted(null);
        if (!StringUtils.hasText(customer.getLevel())) {
            customer.setLevel("C");
        }
        customer.setOwnerId(dataScopeService.resolveOwnerForCreate(customer.getOwnerId()));
        save(customer);
    }

    @Override
    public void updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "客户 ID 不能为空");
        }
        Customer old = getAccessible(customer.getId());
        customer.setOwnerId(dataScopeService.resolveOwnerForUpdate(old.getOwnerId(), customer.getOwnerId()));
        customer.setCreateTime(null);
        customer.setUpdateTime(null);
        customer.setDeleted(null);
        if (!StringUtils.hasText(customer.getLevel())) {
            customer.setLevel("C");
        }
        updateById(customer);
    }

    @Override
    public void deleteCustomer(Long id) {
        getAccessible(id);
        removeById(id);
    }

    @Override
    public ExcelImportResult importCustomers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要导入的 Excel 文件");
        }
        List<CustomerExcelRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(CustomerExcelRow.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Excel 解析失败，请使用模板文件");
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "单次最多导入 " + MAX_IMPORT_ROWS + " 条");
        }

        ExcelImportResult result = new ExcelImportResult();
        result.setTotal(rows.size());
        List<Customer> valid = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();
        Set<String> existingNames = list(new LambdaQueryWrapper<Customer>()
                .select(Customer::getName))
                .stream().map(Customer::getName).collect(Collectors.toSet());
        Long ownerId = dataScopeService.resolveOwnerForCreate(null);

        for (int i = 0; i < rows.size(); i++) {
            CustomerExcelRow row = rows.get(i);
            int rowNo = i + 2;
            String error = validateRow(row);
            if (error == null && namesInFile.contains(row.getName().trim())) {
                error = "文件内客户名称重复";
            }
            if (error == null && existingNames.contains(row.getName().trim())) {
                error = "客户已存在";
            }
            if (error != null) {
                result.getErrors().add("第 " + rowNo + " 行：" + error);
                continue;
            }
            namesInFile.add(row.getName().trim());
            Customer customer = new Customer();
            customer.setName(row.getName().trim());
            customer.setIndustry(row.getIndustry());
            customer.setLevel(StringUtils.hasText(row.getLevel()) ? row.getLevel().trim().toUpperCase() : "C");
            customer.setSource(row.getSource());
            customer.setAddress(row.getAddress());
            customer.setRemark(row.getRemark());
            customer.setOwnerId(ownerId);
            valid.add(customer);
        }

        if (!valid.isEmpty()) {
            saveBatch(valid);
        }
        result.setSuccess(valid.size());
        result.setFailed(rows.size() - valid.size());
        return result;
    }

    @Override
    public void exportCustomers(CustomerQueryDTO dto, OutputStream outputStream) {
        List<Long> scopeIds = dataScopeService.accessibleUserIds();
        List<Customer> records = (scopeIds != null && scopeIds.isEmpty())
                ? List.of()
                : list(buildWrapper(dto, scopeIds).last("LIMIT " + MAX_EXPORT_ROWS));
        List<CustomerExcelRow> rows = records.stream().map(c -> {
            CustomerExcelRow row = new CustomerExcelRow();
            row.setName(c.getName());
            row.setIndustry(c.getIndustry());
            row.setLevel(c.getLevel());
            row.setSource(c.getSource());
            row.setAddress(c.getAddress());
            row.setRemark(c.getRemark());
            return row;
        }).collect(Collectors.toList());
        EasyExcel.write(outputStream, CustomerExcelRow.class).sheet("客户").doWrite(rows);
    }

    private LambdaQueryWrapper<Customer> buildWrapper(CustomerQueryDTO dto, List<Long> scopeIds) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getName())) {
            wrapper.like(Customer::getName, dto.getName());
        }
        if (StringUtils.hasText(dto.getIndustry())) {
            wrapper.eq(Customer::getIndustry, dto.getIndustry());
        }
        if (StringUtils.hasText(dto.getLevel())) {
            wrapper.eq(Customer::getLevel, dto.getLevel());
        }
        if (dto.getOwnerId() != null) {
            wrapper.eq(Customer::getOwnerId, dto.getOwnerId());
        }
        if (scopeIds != null) {
            wrapper.in(Customer::getOwnerId, scopeIds);
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        return wrapper;
    }

    private void fillOwnerName(List<Customer> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> ownerIds = records.stream().map(Customer::getOwnerId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        records.forEach(c -> c.setOwnerName(nameMap.getOrDefault(c.getOwnerId(), "")));
    }

    private String validateRow(CustomerExcelRow row) {
        if (!StringUtils.hasText(row.getName())) {
            return "客户名称不能为空";
        }
        String name = row.getName().trim();
        if (name.length() > 128) {
            return "客户名称超过 128 字";
        }
        if (row.getLevel() != null && !row.getLevel().isBlank()
                && !row.getLevel().trim().toUpperCase().matches("[ABCD]")) {
            return "客户级别只能是 A/B/C/D";
        }
        if (row.getIndustry() != null && row.getIndustry().length() > 64) {
            return "行业超过 64 字";
        }
        if (row.getSource() != null && row.getSource().length() > 64) {
            return "客户来源超过 64 字";
        }
        if (row.getAddress() != null && row.getAddress().length() > 255) {
            return "地址超过 255 字";
        }
        if (row.getRemark() != null && row.getRemark().length() > 500) {
            return "备注超过 500 字";
        }
        return null;
    }
}
