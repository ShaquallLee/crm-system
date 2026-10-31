package com.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.entity.Contact;

import java.util.List;

public interface ContactService extends IService<Contact> {
    List<Contact> listByCustomerId(Long customerId);
}
