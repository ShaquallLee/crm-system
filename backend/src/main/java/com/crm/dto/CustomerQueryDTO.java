package com.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CustomerQueryDTO {
    private String name;
    private String industry;
    private Long ownerId;
    private String level;

    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer size = 10;
}
