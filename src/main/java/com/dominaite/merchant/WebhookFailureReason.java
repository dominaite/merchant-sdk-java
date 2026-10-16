package com.dominaite.merchant;

/** Why a webhook delivery failed verification. */
public enum WebhookFailureReason {
    /** The signature header does not follow the grammar. */
    MALFORMED_SIGNATURE,
    /** The signature does not match the body and the secret. */
    SIGNATURE_MISMATCH,
    /** The signature is valid, but the timestamp is outside the tolerance: a replay. */
    TIMESTAMP_OUT_OF_TOLERANCE,
    /** The body is not a readable event envelope. */
    MALFORMED_PAYLOAD,
}
