package com.shady.landing.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Builds page requests with clamped bounds so clients cannot request huge pages. */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private PageRequests() {
    }

    public static PageRequest of(Integer page, Integer size, Sort sort) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, sort);
    }
}
