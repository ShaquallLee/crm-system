package com.crm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class CustomerExcelRow {
    @ColumnWidth(28)
    @ExcelProperty("客户名称（必填）")
    private String name;

    @ColumnWidth(16)
    @ExcelProperty("行业")
    private String industry;

    @ColumnWidth(16)
    @ExcelProperty("客户级别（A/B/C/D）")
    private String level;

    @ColumnWidth(16)
    @ExcelProperty("客户来源")
    private String source;

    @ColumnWidth(32)
    @ExcelProperty("地址")
    private String address;

    @ColumnWidth(40)
    @ExcelProperty("备注")
    private String remark;
}
