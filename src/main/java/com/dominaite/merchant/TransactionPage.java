package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * One page of {@link DominaiteClient#listTransactions}.
 */
public final class TransactionPage {

    private List<TransactionItem> items;
    private String nextCursor;
    @JsonIgnore
    private JsonNode raw;

    private TransactionPage() {
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    /** The cursor of the next page, or null on the last page. */
    public String getNextCursor() {
        return nextCursor;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
