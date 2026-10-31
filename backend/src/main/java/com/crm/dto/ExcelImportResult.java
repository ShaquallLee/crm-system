package com.crm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExcelImportResult {
    private int total;
    private int success;
    private int failed;
    private List<String> errors = new ArrayList<>();
}
