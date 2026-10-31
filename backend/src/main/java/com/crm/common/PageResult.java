package com.crm.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {
    private Long total;
    private Long current;
    private Long size;
    private List<T> records;

    public PageResult() {}

    public PageResult(IPage<T> page) {
        this.total = page.getTotal();
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.records = page.getRecords();
    }
}
