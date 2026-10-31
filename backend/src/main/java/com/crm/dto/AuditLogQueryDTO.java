package com.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AuditLogQueryDTO {
    private String username;
    private String module;
    private String action;
    private Integer success;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer size = 10;
}
