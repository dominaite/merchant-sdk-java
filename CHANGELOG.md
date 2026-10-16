# Changelog

## 0.5.2

The first release of the Java SDK. It has the same API surface as version 0.5.2 of the Node,
Python, Go, .NET, PHP and Rust SDKs. The 0.5.1 entry below lists what it has from that version.

### Added

- Billing address on the payer: `Customer.address`, `city`, `postalCode` and `state`, all
  optional. Address is one line, at most 200 characters; city at most 100; postal code at most
  20. State is the state or province code, at most 50 characters, and only used for the United
  States, Canada and Australia. The widget does not ask the payer for a field that you send. A
  value that is too long fails with 400 `VALIDATION_ERROR`, code `MAX_LENGTH`.

## 0.5.1

### Added

- `DominaiteClient` with a builder: `keyId`, `secret`, `baseUrl`, `timeout`, `userAgentSuffix`
  and an optional `java.net.http.HttpClient`. Java 11 or later. Jackson databind is the only
  dependency.
- Every merchant API route: `ping`, `getIntegrationContract`, `createCheckoutSession`,
  `createCheckoutSessionWithRetry`, `getStatus`, `chargePaymentMethod`, `revokePaymentMethod`,
  `createRefund`, `getRefund`, `listRefunds`, `createPlan`, `listPlans`, `getPlan`, `updatePlan`,
  `deletePlan`, `createAgreement`, `getAgreement`, `cancelAgreement`, `createWebhookEndpoint`,
  `listWebhookEndpoints`, `deleteWebhookEndpoint` and `listTransactions`.
- `TransactionItem` has `getFee()`, `getNetAmount()`, `getSettledAt()` and `getPayoutId()`. They
  are null until the acquirer settles the payment. `getNetAmount()` is the amount minus the fee of
  this payment only.
- `ListTransactionsRequest` has `settledFrom` and `settledTo` (`LocalDate`), sent as `yyyy-MM-dd`
  query parameters. Set both or neither.
- `Webhooks.verify` and `Webhooks.verifyAndParse`: a constant-time MAC check, then a 300 second
  timestamp tolerance, with typed data for `payment.*`, `verification.*`, `agreement.*` and
  `charge.*` events.
- `RequestSigner`, `IdempotencyKeys` and `MinorUnits`, the same as in the other SDKs.
- Unchecked exceptions under `DominaiteException`. A charge decline is a result, not an exception.
  Every exception from the API has `getRequestId()`.
