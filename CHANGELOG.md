# Changelog

## 0.7.0 (unreleased)

### Breaking

- The client retries by itself. `Builder.maxRetries(int)` sets the count, default 2 (3 attempts),
  for every GET call and every POST call with an idempotency key. It retries a transport failure,
  a 5xx without a code of the route and `PAYMENT_PROCESSING_UNAVAILABLE`, never a 429. The wait
  before retry n is random between 0 and 500 ms x 2^n, at most 8 seconds. The timeout is per
  attempt, so one call can now wait up to (maxRetries + 1) x timeout plus the waits. Set
  `maxRetries(0)` to send every call once, as before. `createWebhookEndpoint`, `updatePlan` and
  the DELETE calls are never retried.
- A charge answered 404 with a code (`PAYMENT_METHOD_NOT_FOUND`, `AGREEMENT_NOT_FOUND`) throws
  `ChargeException`, not a plain `ApiException`. `ChargeException` already extends `ApiException`,
  so a `catch (ApiException e)` still catches it. Check the order of your catch blocks and
  `instanceof` tests: a `ChargeException` branch now also gets these 404s.
- A charge or revoke answered 5xx with a code the route does not own, such as `INTERNAL_ERROR`,
  throws `TransportException` with that code, not `ChargeException` or `RevokeException`. The
  client retries it.
- `createPlan` throws `ValidationException` and sends nothing when the amount is not positive. An
  unset amount was sent as 0.
- `ErrorCodes.CHARGE`, `ErrorCodes.SESSION_REFUSALS` and `ErrorCodes.REFUND` list every code the
  gateway sends on these routes. Code that compares them to a fixed list sees more entries.

### Added

- `getRequestId()` on every result type, through the new base class `ApiResult`. Results and
  errors read the `Dominaite-Request-Id` response header first, then `metadata.requestId` in the
  body. Production sends only the body field today. An error without a JSON body takes the header.
- `DominaiteClient.DEFAULT_MAX_RETRIES`, `RETRY_BASE_DELAY`, `RETRY_MAX_DELAY` and
  `getMaxRetries()`.
- `ChargeEventData.getPaymentMethod()`: the brand and last four digits of a `charge.*` event.
- `ErrorCodes` constants for the charge, agreement, plan, webhook, validation and generic codes,
  and the lists `AUTHENTICATION`, `IDEMPOTENCY`, `TRANSIENT`, `VALIDATION`, `RECURRING` and
  `WEBHOOK`. A test pins them to the vendored wire contract.
- `WebhookEventTypes.RECURRING`: the subscription events, in the contract's order.

- `createCheckoutSession` and `chargePaymentMethod` throw `ValidationException` and send nothing
  when the currency is not 3 upper case letters. The gateway refused it with a 400.

### Fixed

- A `pm_` payment method id typed in upper case is lowercased before signing. The gateway signs
  the lowercase id, so the call failed with `INVALID_SIGNATURE`.
- Instants in queries and bodies are cut to microseconds. An `Instant` with nanoseconds printed 9
  fractional digits in `from` and `to`, and the gateway reads at most 7.

### Deprecated

- `createCheckoutSessionWithRetry`. `createCheckoutSession` now retries by itself. The helper
  still works with its own attempt count in place of the client's, and now waits with the same
  full jitter, at most 8 seconds per wait.

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
