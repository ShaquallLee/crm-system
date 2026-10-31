package com.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.common.PageResult;
import com.crm.dto.CustomerQueryDTO;
import com.crm.dto.ExcelImportResult;
import com.crm.entity.Customer;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface CustomerService extends IService<Customer> {
    PageResult<Customer> pageQuery(CustomerQueryDTO dto);

    Customer getAccessible(Long id);

    void createCustomer(Customer customer);

    void updateCustomer(Customer customer);

    void deleteCustomer(Long id);

    ExcelImportResult importCustomers(MultipartFile file);

    void exportCustomers(CustomerQueryDTO dto, OutputStream outputStream);
}
