package edu.fpt.sba301.bookstore.support;

import edu.fpt.sba301.bookstore.constant.PaginationConstants;
import org.springframework.data.domain.PageRequest;

public final class PaginationSupport {

    private PaginationSupport() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, PaginationConstants.DEFAULT_PAGE);
    }

    public static int normalizeSize(int size) {
        return Math.min(Math.max(size, PaginationConstants.MIN_PAGE_SIZE), PaginationConstants.MAX_PAGE_SIZE);
    }

    public static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(normalizePage(page), normalizeSize(size));
    }
}
