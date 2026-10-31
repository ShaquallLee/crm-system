package com.crm.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 统一分页参数兜底，防止超大 size 拖库
 */
public final class PageUtil {

    public static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    public static <T> Page<T> of(Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, MAX_PAGE_SIZE);
        return new Page<>(current, pageSize);
    }
}
