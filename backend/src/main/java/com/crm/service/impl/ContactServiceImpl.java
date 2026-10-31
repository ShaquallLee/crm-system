package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.entity.Contact;
import com.crm.mapper.ContactMapper;
import com.crm.service.ContactService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements ContactService {

    @Override
    public List<Contact> listByCustomerId(Long customerId) {
        return list(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .orderByDesc(Contact::getIsPrimary)
                .orderByDesc(Contact::getCreateTime));
    }
}
