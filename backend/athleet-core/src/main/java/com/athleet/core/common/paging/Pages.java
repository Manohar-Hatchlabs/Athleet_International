package com.athleet.core.common.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Builds bounded {@link Pageable}s. Sort fields come only from an allow-list map (API name -> entity property),
 * so client input never reaches a query as a property name.
 */
public final class Pages {

    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    public static Pageable of(int page, int size, String sort, Map<String, String> allowedSorts, Sort defaultSort) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(p, s, parseSort(sort, allowedSorts, defaultSort));
    }

    private static Sort parseSort(String sort, Map<String, String> allowed, Sort fallback) {
        if (sort == null || sort.isBlank()) {
            return fallback;
        }
        String[] parts = sort.split(",", 2);
        String property = allowed.get(parts[0].trim());
        if (property == null) {
            return fallback;
        }
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, property).and(Sort.by("id"));
    }
}
