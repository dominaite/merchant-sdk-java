# Changelog

## 0.6.0

### Breaking

- The base URL is required. The client no longer falls back to production when you leave it
  out, so an unset environment variable cannot send sandbox credentials to production.
  `build()` throws `ValidationException` when the base URL is missing, null, empty or
  whitespace. Pass `DominaiteClient.SANDBOX_BASE_URL` (`https://api.staging.dominaite.com/payments`)
  or `DominaiteClient.PRODUCTION_BASE_URL` (`https://api.dominaite.com/payments`), or your own value.
- `DominaiteClient.DEFAULT_BASE_URL` is removed. Use `DominaiteClient.PRODUCTION_BASE_URL`.
- The base URL must be `https://`. `http://` is accepted only for localhost, 127.0.0.1 and ::1,
  the same rule as the other SDKs. `build()` throws `ValidationException` for anything else.

### Added

- User-Agent `dominaite-java/<version> (java <java.version>)`, then the `userAgentSuffix` after one
  space. A suffix with a control character throws `ValidationException`.
- Warning hook: `Builder.onWarning(Consumer<String>)`. Without it, warnings go to
  `System.getLogger("com.dominaite.merchant")` at `WARNING`. The SDK warns when the
  `Dominaite-Sdk-Notice` response header marks this version as deprecated (once per distinct
  notice), when `createCheckoutSessionWithRetry` retries after a failure, and when `ping()` reports
  a clock skew over 60 seconds.
- Debug log: `Builder.onDebug(Consumer<String>)`, off by default. One line per request with the
  method, the path, the status, the time in milliseconds, the `Dominaite-Request-Id` response
  header and the idempotency key. No line holds the secret, the signature, a cashier token or a body.

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
