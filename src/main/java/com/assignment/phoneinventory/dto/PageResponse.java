package com.assignment.phoneinventory.dto;

import java.util.List;

public class PageResponse<T> {
    public final List<T> content;
    public final int page;
    public final int size;
    public final long totalElements;
    public final long totalPages;

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (long) Math.ceil((double) totalElements / (size <= 0 ? 1 : size));
    }
}
