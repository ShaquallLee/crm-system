package com.crm.util;

import com.crm.common.BusinessException;
import com.crm.common.ResultCode;

import java.util.regex.Pattern;

/**
 * 密码强度策略：8-32 位，必须同时包含字母和数字
 */
public final class PasswordPolicy {

    private static final Pattern RULE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,32}$");

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || !RULE.matcher(password).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "密码需为 8-32 位，且同时包含字母和数字");
        }
    }
}
