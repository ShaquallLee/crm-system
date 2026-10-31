package com.crm.controller;

import com.alibaba.excel.EasyExcel;
import com.crm.common.PageResult;
import com.crm.common.Result;
import com.crm.common.annotation.Audit;
import com.crm.dto.CustomerExcelRow;
import com.crm.dto.CustomerQueryDTO;
import com.crm.dto.ExcelImportResult;
import com.crm.entity.Customer;
import com.crm.service.CustomerService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Result<PageResult<Customer>> list(@Valid CustomerQueryDTO dto) {
        return Result.success(customerService.pageQuery(dto));
    }

    @GetMapping("/export")
    @Audit(module = "客户", action = "导出 Excel")
    public void export(@Valid CustomerQueryDTO dto, HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("客户数据_" + LocalDate.now() + ".xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);
        customerService.exportCustomers(dto, response.getOutputStream());
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("客户导入模板.xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);
        EasyExcel.write(response.getOutputStream(), CustomerExcelRow.class)
                .sheet("客户").doWrite(List.of());
    }

    @PostMapping("/import")
    @Audit(module = "客户", action = "导入 Excel")
    public Result<ExcelImportResult> importCustomers(@RequestParam("file") MultipartFile file) {
        return Result.success(customerService.importCustomers(file));
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.success(customerService.getAccessible(id));
    }

    @PostMapping
    @Audit(module = "客户", action = "新增客户")
    public Result<Void> create(@Valid @RequestBody Customer customer) {
        customerService.createCustomer(customer);
        return Result.success();
    }

    @PutMapping
    @Audit(module = "客户", action = "编辑客户")
    public Result<Void> update(@Valid @RequestBody Customer customer) {
        customerService.updateCustomer(customer);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "客户", action = "删除客户")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.success();
    }
}
