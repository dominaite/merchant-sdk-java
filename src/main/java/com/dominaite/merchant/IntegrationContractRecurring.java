package com.dominaite.merchant;

import java.util.List;

/**
 * The subscription vocabularies of the integration contract.
 */
public final class IntegrationContractRecurring {

    private List<String> consentTextVersions;
    private List<String> intervalUnits;
    private List<String> planStatuses;
    private List<String> agreementStatuses;

    private IntegrationContractRecurring() {
    }

    public List<String> getConsentTextVersions() {
        return consentTextVersions;
    }

    public List<String> getIntervalUnits() {
        return intervalUnits;
    }

    public List<String> getPlanStatuses() {
        return planStatuses;
    }

    public List<String> getAgreementStatuses() {
        return agreementStatuses;
    }
}
