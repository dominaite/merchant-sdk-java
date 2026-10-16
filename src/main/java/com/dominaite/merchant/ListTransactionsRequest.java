package com.dominaite.merchant;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The query of {@link DominaiteClient#listTransactions}. The query is sent in the URL and is
 * never part of the signed path. Immutable: build it with {@link #builder()}.
 */
public final class ListTransactionsRequest {

    private final Instant from;
    private final Instant to;
    private final LocalDate settledFrom;
    private final LocalDate settledTo;
    private final String status;
    private final Integer limit;
    private final String cursor;
    private final Map<String, String> extra;

    private ListTransactionsRequest(Builder b) {
        this.from = b.from;
        this.to = b.to;
        this.settledFrom = b.settledFrom;
        this.settledTo = b.settledTo;
        this.status = b.status;
        this.limit = b.limit;
        this.cursor = b.cursor;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.from = from;
        b.to = to;
        b.settledFrom = settledFrom;
        b.settledTo = settledTo;
        b.status = status;
        b.limit = limit;
        b.cursor = cursor;
        b.extra.putAll(extra);
        return b;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public LocalDate getSettledFrom() {
        return settledFrom;
    }

    public LocalDate getSettledTo() {
        return settledTo;
    }

    public String getStatus() {
        return status;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getCursor() {
        return cursor;
    }

    /**
     * The same query for the next page.
     *
     * @param nextCursor {@link TransactionPage#getNextCursor()} of the page you just read
     * @return a new request with only the cursor changed
     */
    public ListTransactionsRequest withCursor(String nextCursor) {
        return toBuilder().cursor(nextCursor).build();
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    /** Builds a {@link ListTransactionsRequest}. */
    public static final class Builder {

        private Instant from;
        private Instant to;
        private LocalDate settledFrom;
        private LocalDate settledTo;
        private String status;
        private Integer limit;
        private String cursor;
        private final Map<String, String> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** Required. The window start, inclusive. */
        public Builder from(Instant from) {
            this.from = from;
            return this;
        }

        /** Required. The window end, exclusive. The window is at most 31 days. */
        public Builder to(Instant to) {
            this.to = to;
            return this;
        }

        /** The first settlement day, inclusive. Set both settledFrom and settledTo, or neither. */
        public Builder settledFrom(LocalDate settledFrom) {
            this.settledFrom = settledFrom;
            return this;
        }

        /** The settlement day after the last one (exclusive). */
        public Builder settledTo(LocalDate settledTo) {
            this.settledTo = settledTo;
            return this;
        }

        /** Only transactions with this status, from {@link TransactionStatuses}. */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /** The page size. */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /** The nextCursor of the previous page, or null for the first page. */
        public Builder cursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        /**
         * A query parameter this SDK version does not model yet. A field the request sets itself
         * wins over an extra one with the same name.
         */
        public Builder extra(String name, String value) {
            this.extra.put(name, value);
            return this;
        }

        public ListTransactionsRequest build() {
            return new ListTransactionsRequest(this);
        }
    }
}
