package com.jenikmax.game.library.model.dto.api;

import java.util.List;

public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int totalPages;
    private long totalItems;
    private int pageSize;

    public PageResponse() {}

    public PageResponse(List<T> items, int page, int totalPages, long totalItems, int pageSize) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
