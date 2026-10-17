package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The public contract of the API: version, status and event values, and the recurring
 * vocabularies. {@link #getRaw()} has the whole contract.
 */
public final class IntegrationContract extends ApiResult {

    private String version;
    private String apiBase;
    private List<String> statuses;
    private List<String> webhookEventCatalog;
    private IntegrationContractRecurring recurring;
    @JsonIgnore
    private JsonNode raw;

    private IntegrationContract() {
    }

    public String getVersion() {
        return version;
    }

    public String getApiBase() {
        return apiBase;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public List<String> getWebhookEventCatalog() {
        return webhookEventCatalog;
    }

    public IntegrationContractRecurring getRecurring() {
        return recurring;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
