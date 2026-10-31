package com.crm.common.aspect;

import com.crm.common.annotation.Audit;
import com.crm.entity.AuditLog;
import com.crm.mapper.AuditLogMapper;
import com.crm.security.LoginUser;
import com.crm.security.UserContext;
import com.crm.util.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final int MAX_PARAM_LENGTH = 1000;

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        boolean success = false;
        String errorMsg = null;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            saveLog(joinPoint, audit, request, success, errorMsg,
                    System.currentTimeMillis() - start);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Audit audit, HttpServletRequest request,
                         boolean success, String errorMsg, long costMs) {
        try {
            AuditLog item = new AuditLog();
            LoginUser user = UserContext.get();
            item.setUserId(user != null ? user.getUserId() : null);
            item.setUsername(user != null ? user.getUsername() : null);
            item.setModule(audit.module());
            item.setAction(audit.action());
            item.setMethod(request != null ? request.getMethod() : null);
            item.setUri(request != null ? request.getRequestURI() : null);
            item.setIp(IpUtil.getClientIp(request));
            item.setParams(serializeArgs(joinPoint.getArgs()));
            item.setSuccess(success ? 1 : 0);
            item.setErrorMsg(errorMsg != null && errorMsg.length() > 500
                    ? errorMsg.substring(0, 500) : errorMsg);
            item.setCostMs(costMs);
            auditLogMapper.insert(item);
        } catch (Exception e) {
            log.warn("写入操作审计日志失败: {}", e.getMessage());
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            List<Object> safe = Arrays.stream(args)
                    .filter(a -> a != null
                            && !(a instanceof ServletRequest)
                            && !(a instanceof ServletResponse)
                            && !(a instanceof MultipartFile)
                            && !(a instanceof BindingResult))
                    .toList();
            String json = objectMapper.writeValueAsString(safe);
            json = json.replaceAll(
                    "(?i)\"(password|oldPassword|newPassword|token)\"\\s*:\\s*\"[^\"]*\"",
                    "\"$1\":\"***\"");
            return json.length() > MAX_PARAM_LENGTH ? json.substring(0, MAX_PARAM_LENGTH) : json;
        } catch (Exception e) {
            return "";
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
