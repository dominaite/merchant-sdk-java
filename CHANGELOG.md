# Changelog

## 0.5.1

The first release of the Java SDK. It has the same API surface as version 0.5.1 of the Node,
Python, Go, .NET, PHP and Rust SDKs.

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
