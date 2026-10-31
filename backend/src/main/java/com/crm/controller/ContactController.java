package com.crm.controller;

import com.crm.common.BusinessException;
import com.crm.common.Result;
import com.crm.common.ResultCode;
import com.crm.common.annotation.Audit;
import com.crm.entity.Contact;
import com.crm.service.ContactService;
import com.crm.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final CustomerService customerService;

    @GetMapping("/customer/{customerId}")
    public Result<List<Contact>> listByCustomer(@PathVariable Long customerId) {
        customerService.getAccessible(customerId);
        return Result.success(contactService.listByCustomerId(customerId));
    }

    @GetMapping("/{id}")
    public Result<Contact> getById(@PathVariable Long id) {
        Contact contact = requireContact(id);
        customerService.getAccessible(contact.getCustomerId());
        return Result.success(contact);
    }

    @PostMapping
    @Audit(module = "联系人", action = "新增联系人")
    public Result<Void> create(@Valid @RequestBody Contact contact) {
        customerService.getAccessible(contact.getCustomerId());
        contact.setId(null);
        contact.setCreateTime(null);
        contact.setUpdateTime(null);
        contact.setDeleted(null);
        contactService.save(contact);
        return Result.success();
    }

    @PutMapping
    @Audit(module = "联系人", action = "编辑联系人")
    public Result<Void> update(@Valid @RequestBody Contact contact) {
        Contact old = requireContact(contact.getId());
        customerService.getAccessible(old.getCustomerId());
        customerService.getAccessible(contact.getCustomerId());
        contact.setCreateTime(null);
        contact.setUpdateTime(null);
        contact.setDeleted(null);
        contactService.updateById(contact);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "联系人", action = "删除联系人")
    public Result<Void> delete(@PathVariable Long id) {
        Contact contact = requireContact(id);
        customerService.getAccessible(contact.getCustomerId());
        contactService.removeById(id);
        return Result.success();
    }

    private Contact requireContact(Long id) {
        Contact contact = contactService.getById(id);
        if (contact == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "联系人不存在");
        }
        return contact;
    }
}
