# Dominaite merchant SDK for Java

A server-side Java client for the Dominaite merchant API. One call from your backend opens a hosted
checkout session. A two-line script tag shows the payment widget on your page. Card details go
from the browser of your customer into the widget. They never touch your server, so your PCI scope
stays minimal (SAQ A).

The SDK needs Java 11 or later. It uses `java.net.http` for HTTP. Its only dependency is Jackson
databind.

## Install

Maven:

```xml
<dependency>
  <groupId>com.dominaite</groupId>
  <artifactId>merchant-sdk</artifactId>
  <version>0.6.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.dominaite:merchant-sdk:0.6.0'
```

The package is `com.dominaite.merchant`. The JAR has the automatic module name
`com.dominaite.merchant`.

To work on the SDK itself:

```sh
./mvnw verify        # includes the offline signing and webhook vectors
```

## Credentials

You get two values from the Dominaite dashboard, **Website integration** tab, when you generate an
API key. The dashboard shows them once. Store them like passwords.

- `dmk_...` is your API key id. It identifies you. It is not secret by itself.
- `dms_...` is your API secret. Keep it on the server only: in an environment variable or a secret
  store. Never put it in a browser, in git or in a log.

The SDK signs every request with the secret (HMAC-SHA256) and puts a timestamp on it. Keep your
server clock on NTP. The API refuses a signature older than 5 minutes with
`TIMESTAMP_OUT_OF_RANGE`.

If the key has an IP allowlist, a call from another address fails with `IP_NOT_ALLOWED`. You
manage the allowlist on the same dashboard tab.

A key has scopes. They are fixed when the key is issued. A payments key (`payments`) makes the
payment, refund and subscription calls. A reporting key holds `transactions:read`,
`webhooks:manage` or both, and never `payments`. It is a second key next to the payments key. It is
for the whole account, and you cannot bind it to a storefront. If your dashboard has no reporting
key section, ask Dominaite support for the key. Every key can call `ping()` and
`getIntegrationContract()`. A call without the scope it needs throws
`InsufficientScopeException` with code `INSUFFICIENT_SCOPE`. A payments key always gets HTTP 403
`INSUFFICIENT_SCOPE` on the webhook endpoint methods and on `listTransactions`.

`DominaiteClient.toString()` hides the secret, so a logged client cannot leak it.
`CheckoutSession.toString()` hides the cashier token and the client secret the same way.

## What we switch on for your account

| What | Without it | How you get it |
|---|---|---|
| Stored-card charges | `chargePaymentMethod` throws `ChargeException` with HTTP 503 `PAYMENT_METHOD_CHARGES_DISABLED`. | Ask Dominaite support. The dashboard has no switch. |
| Subscription sessions | `createCheckoutSession` with `agreementId` throws `RefusalException` with `SUBSCRIPTIONS_NOT_ENABLED`. | Ask Dominaite support. The dashboard has no switch. |
| Scheduled charges | The platform runs no scheduled charge and no retry, even when the subscription session works. | Ask Dominaite support. The dashboard has no switch. |
| Verify-only sessions | `createCheckoutSession` with `verifyOnly` throws `PermissionException` with HTTP 403 `VERIFY_ONLY_NOT_ENABLED`. | Ask Dominaite support. The dashboard has no switch. |
| Card fields | See [Card fields](#card-fields). | Ask Dominaite support. |
| Reporting key | A payments key always gets HTTP 403 `INSUFFICIENT_SCOPE` on the webhook endpoint methods and on `listTransactions`. | See [Credentials](#credentials). |

## Environments

| Environment | Base URL |
|---|---|
| Sandbox | `https://api.staging.dominaite.com/payments` (`DominaiteClient.SANDBOX_BASE_URL`) |
| Production | `https://api.dominaite.com/payments` (`DominaiteClient.PRODUCTION_BASE_URL`) |

The base URL is required: there is no default. A missing, empty or whitespace-only `baseUrl`
throws `ValidationException` when you build the client. Keys are issued per environment, so
**a sandbox key against production always fails with `INVALID_API_KEY`**.

`DominaiteClient.DEFAULT_BASE_URL` is removed in 0.6.0. Use `DominaiteClient.PRODUCTION_BASE_URL`.

The prefix of the base URL is never part of the signed path. On production you POST to
`https://api.dominaite.com/payments/merchant-api/checkout/sessions`, but you sign
`/merchant-api/checkout/sessions`.

## Quick start

```sh
export DOMINAITE_KEY_ID=dmk_...      # Website integration tab
export DOMINAITE_SECRET=dms_...      # shown once when you generated the key
# Required. The sandbox, or https://api.dominaite.com/payments for production:
export DOMINAITE_BASE_URL=https://api.staging.dominaite.com/payments
```

```java
import com.dominaite.merchant.*;

DominaiteClient client = DominaiteClient.builder()
    .keyId(System.getenv("DOMINAITE_KEY_ID"))
    .secret(System.getenv("DOMINAITE_SECRET"))
    // Required. An unset variable is null, and build() throws on it.
    .baseUrl(System.getenv("DOMINAITE_BASE_URL"))
    .build();

// First live call: it proves the key, the secret, the signing and the clock, and creates nothing.
PingResponse ping = client.ping();
System.out.println("merchant " + ping.getMerchantId() + ", clock skew " + ping.getClockSkewSeconds() + "s");

CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
    .amount(2500) // 2500 = 25.00 EUR, always MINOR units
    .currency("EUR")
    .orderReference("order-1042")
    .customer(Customer.builder().firstName("Ana").email("ana@example.com").build())
    // Required. The same order at the same amount gives the same key,
    // so a reload or a retry replays this session.
    .idempotencyKey(IdempotencyKeys.forOrder("checkout", "order-1042", 2500, "EUR"))
    .build());

// Store session.getTransactionId() with your order. Then give the cashier key and the
// cashier token to the page that shows the widget.
```

`customer` is optional. Send what your checkout already collected: name, email, phone and the
billing address (`address`, `city`, `postalCode`, and `state` for the United States, Canada and
Australia). The widget does not ask the payer for a field that you send. A value that is too long
fails with 400 `VALIDATION_ERROR`, code `MAX_LENGTH`.

Show the widget with the two cashier values:

```html
<div id="checkout">
  <script src="https://bp-checkout.dominaite.com/v2/launcher"
          data-cashier-key="CASHIER_KEY_FROM_SESSION"
          data-cashier-token="CASHIER_TOKEN_FROM_SESSION"></script>
</div>
```

The launcher shows the form where the script tag is, so keep the script inside your container. The
cashier key and the cashier token are values for one payment session, not credentials. HTML-escape
them when you put them in the page, and **never log the token**.

### Then find out whether it got paid

Opening the session is half of the integration. The widget runs in the browser of your customer,
so the call above does not tell your server the result.

Register a webhook endpoint, verify every delivery with `Webhooks.verify`, and fulfil the order
when `payment.succeeded` arrives. See [Webhooks](#webhooks). If you cannot receive inbound requests
yet, poll `getStatus` instead, and move to webhooks when you can.

In both cases, keep a reconciliation sweep. Webhooks are the fast path, not the guarantee.

## Verify your signing before your first live call

Run `./mvnw verify` before you call the live API. The SDK signs for you. Offline known-answer
vectors pin the recipe. The gateway, the dashboard and the other Dominaite SDKs share these
vectors, and the test suite reproduces them byte for byte. If one fails, every live call fails with
`INVALID_SIGNATURE`.

`RequestSigner.sign` is public. Use it to pin the recipe in your own tests, or to debug an
`INVALID_SIGNATURE`:

```java
String signature = RequestSigner.sign(new SignatureInput(
    "dms_...",
    "1755302400",                                   // unix SECONDS
    "POST",
    "/merchant-api/checkout/sessions",              // the path only, no host
    "00000000-0000-4000-8000-000000000001",         // "" for GET
    "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1042\"}")); // "" for GET
// "8f5fba0b29a8eea81b76a0e6d7119e79ec68f586910f77713b045652e5ce9b74"
```

The signed payload has five lines:
`"{timestamp}\n{METHOD}\n{path}\n{idempotencyKey}\n{sha256hex(body)}"`. The signature is the
lowercase hex HMAC-SHA256 of the payload with your secret. Everything is UTF-8. Two rules:

- GET, DELETE and PATCH sign an EMPTY idempotency key and send no `Idempotency-Key` header. GET and
  DELETE also sign an empty body. The payload still has five lines.
- The signed path never includes the prefix of the base URL or the query string.

The SDK serializes the body once. The bytes that it hashes are the bytes that it sends. So a
non-ASCII payer name cannot give a signature over one encoding and a request in another.

## Ping before your first session

```java
PingResponse ping = client.ping();
```

`ping()` is a GET that creates nothing and reads nothing. It returns `isPong()`, your merchant id,
the server time, the server Unix time and the clock skew in seconds (the server time minus your
`X-Timestamp`). If the absolute skew moves toward 300, fix NTP now. Requests start to fail at 300.

Create your first session only after ping returns. A 401 there means the key id, the secret or the
signing. A 503 means retry later.

## Client options

`DominaiteClient.builder().keyId(...).secret(...).baseUrl(...).build()` gives you a client on that
base URL with a 45 second timeout. The builder takes:

| Option | What |
|---|---|
| `baseUrl` | Required. `DominaiteClient.SANDBOX_BASE_URL` or `DominaiteClient.PRODUCTION_BASE_URL`, or your own value. Null, empty or whitespace throws `ValidationException`. Must be `https://`; `http://` only for localhost, 127.0.0.1 and ::1. |
| `timeout` | The timeout for each attempt. The default is 45 seconds, because serverless cold starts can take 10 seconds or more. With retries, one call can wait up to `(maxRetries + 1) x timeout`, plus the waits between attempts. |
| `maxRetries` | How many times the client retries a failed call after the first attempt. The default is 2; 0 sends every call once. A negative value throws `ValidationException`. See [Retries and double charges](#retries-and-double-charges). |
| `userAgentSuffix` | Adds your identifier to the User-Agent, after one space. This helps when support reads the access logs. A control character throws `ValidationException`. |
| `onWarning` | A `Consumer<String>` for the SDK's warnings. See [Logging and warnings](#logging-and-warnings). |
| `onDebug` | A `Consumer<String>` that switches the debug log on. Off by default. See [Logging and warnings](#logging-and-warnings). |
| `httpClient` | Your own `java.net.http.HttpClient`, for a proxy or your own executor. Build it with `followRedirects(HttpClient.Redirect.NEVER)`. The SDK still sets `timeout` on each request. |

The SDK sends the User-Agent `dominaite-java/<version> (java <java.version>)`, for example
`dominaite-java/0.6.0 (java 21.0.4)`, then your suffix.

Make one client for each process. The client is thread-safe and holds one `HttpClient` with its
connection pool.

The client that the SDK builds never follows a redirect, and it treats any 3xx as a hard error that
is not retryable. The Dominaite API never redirects. A 3xx means that something in front of the API
answered. If the client followed it, it would send your signed request again to the host that the
redirect names.

## Logging and warnings

The SDK writes nothing by itself except warnings. Warnings go to `onWarning`, or without it to
`System.getLogger("com.dominaite.merchant")` at `WARNING`. You get a warning when:

- the API marks your SDK version as deprecated with the `Dominaite-Sdk-Notice` response header.
  The SDK reads it on every response and warns once for each distinct notice, for example
  `dominaite-java 0.6.0 is deprecated; upgrade to 0.7.0 (sunset 2027-01-31). https://...`;
- the client retries a call after a failure;
- `ping()` reports a clock skew over 60 seconds.

`onDebug` switches on the debug log, one line per HTTP request: the method, the path, the status,
the time in milliseconds, the `Dominaite-Request-Id` response header and the idempotency key.
Give the request id to support when you ask about a call.

```java
DominaiteClient client = DominaiteClient.builder()
    .keyId(System.getenv("DOMINAITE_KEY_ID"))
    .secret(System.getenv("DOMINAITE_SECRET"))
    .baseUrl(System.getenv("DOMINAITE_BASE_URL"))
    .onWarning(message -> log.warn(message))
    .onDebug(line -> log.debug(line))   // leave out in production
    .build();
```

No line holds the secret, the signature, a cashier token, a query string or a request or
response body. An exception thrown by your hook is dropped and never fails the call.

## Amounts are minor units

`amount` is always an integer in the minor unit of the currency: `2500` is 25.00 EUR. The field is
a `long`, so a decimal does not compile. The SDK refuses a value that is not positive before it
sends anything. The server locks the amount. What you send is what the payer pays, and nothing in
the browser can change it. Calculate it from your own catalog, never from the request that your
page sent you.

The minor-unit exponent depends on the currency. EUR has 2 decimals, JPY has 0 (so `2500` is JPY
2,500) and KWD has 3. **HUF has 0 on the Dominaite gateway** (whole forints, so `1500` is 1,500
HUF). This is not the 2 of ISO 4217. Never hardcode a x100 conversion. `MinorUnits` does it for
you, with the exponents of the gateway:

```java
long amount = MinorUnits.from(new BigDecimal("0.30"), "EUR");  // 30
long yen = MinorUnits.from(new BigDecimal("2500"), "JPY");     // 2500
int decimals = MinorUnits.exponent("KWD");                     // 3
```

It takes `BigDecimal`, never `double` (`0.1 + 0.2` in floating point is not `0.3`). A price with
more decimals than the currency has throws `ValidationException`. The SDK does not round it, even
when the extra digits are zeros (`25.000` EUR, `2500.00` JPY). A negative price throws too. ISK,
KRW, OMR, JOD and TND throw as not supported: the gateway and ISO 4217 do not agree on them, and a
wrong guess is a silent 100x or 10x charge. Any other currency without a known exponent throws too.
`MinorUnits.currencies()` lists the ones it knows: EUR, USD, GBP, CAD, AUD, CHF, BGN, RON, PLN,
CZK, SEK, DKK, NOK (2), JPY, HUF (0), BHD, KWD (3).

## Retries and double charges

Every `createCheckoutSession` and `chargePaymentMethod` call needs an idempotency key, and the SDK
never makes one up. A null or blank key throws `ValidationException` before anything is sent.
Derive the key from the order:

```java
String key = IdempotencyKeys.forOrder("checkout", order.getId(), amountMinor, "EUR");
// "checkout-order-1042-2500-EUR"
```

The key is `{scope}-{orderId}-{amountMinor}-{CURRENCY}`. The same order at the same amount always
gives the same key. So a page reload, a back button or a retried POST replays the session that
already exists, and does not open a second payment. A changed amount or currency gives a new key.
The gateway wants this: it refuses a reused key with a different amount as
`IDEMPOTENCY_KEY_REUSED`. Use a different scope for each kind of call (`checkout` for sessions,
`charge` for stored-card charges). The SDK checks the key against the same rules as a key that you
build yourself: 1 to 100 characters, visible ASCII only (`!` to `~`, so no spaces and no non-ASCII
order ids).

The client retries by itself. One policy covers every GET call and every POST call that carries an
idempotency key: `createCheckoutSession`, `chargePaymentMethod`, `createRefund`, `createPlan` and
`createAgreement`.

- It retries transport failures: a network error, a timeout, and a 5xx without a code of the route,
  such as `MERCHANT_API_UNAVAILABLE` or `INTERNAL_ERROR`. It also retries
  `PAYMENT_PROCESSING_UNAVAILABLE`, which means that card payments are off for a moment and nothing
  was created.
- It retries at most `maxRetries` times (default 2, so 3 attempts). `maxRetries(0)` sends every
  call once.
- The wait before retry `n` (from 0) is random between 0 and `500 ms x 2^n`, and at most 8 seconds.
- Every attempt sends the same path, idempotency key and body bytes, with a fresh timestamp and
  signature. A retry never opens a second payment.
- The timeout is per attempt. One call can wait up to `(maxRetries + 1) x timeout`, plus the waits.
- It never retries a rate limit (HTTP 429). It throws `RateLimitedException` at once: wait
  `getRetryAfter()`, then call again with the same key.
- It never retries a refusal with a code of the route, such as `CHARGE_OUTCOME_UNKNOWN`, nor a
  storefront, authentication or permission failure, because a retry does not change them.
- It never retries `createWebhookEndpoint`, `updatePlan` or a DELETE call. A replay of a webhook
  endpoint create answers without the signing secret, so an automatic retry could lose it.

When the retries run out, the client throws the last error. `isRetryable()` on any
`DominaiteException` tells you whether a later call with the same key can still succeed, for your
own retry loop. The client logs each retry as a warning.

```java
DominaiteClient client = DominaiteClient.builder()
    .keyId(System.getenv("DOMINAITE_KEY_ID"))
    .secret(System.getenv("DOMINAITE_SECRET"))
    .baseUrl(System.getenv("DOMINAITE_BASE_URL"))
    .maxRetries(3)   // the default is 2; 0 turns retries off
    .build();
```

`createCheckoutSessionWithRetry` is deprecated. It still works, with its own attempt count in
place of the client's, so the two never add up.

The reused key makes the retry safe: a retried key never opens a second payment. A clean replay of
a session that is still open returns the ORIGINAL session, with the same transaction id and cashier
values, as a normal success. When the earlier attempt has moved on, the replay is a refusal that
names the transaction it collided with. The code is `ALREADY_PROCESSED` if its payment took money,
`PRIOR_ATTEMPT_FAILED` if it ended unpaid, and `DUPLICATE_REQUEST` while it is unfinished but the
API cannot give its session back. So a retry after a timeout either succeeds, or gives you the
transaction id to read with `getStatus`:

```java
try {
    CheckoutSession session = client.createCheckoutSession(request);
} catch (RefusalException error) {
    if (error.getTransactionId() != null) {
        CheckoutStatus status = client.getStatus(error.getTransactionId());
        // Now you know what the earlier attempt did.
    }
}
```

`getTransactionId()` is null when the API did not name one. For example, a `DUPLICATE_REQUEST` from
a concurrent race knows that the key is taken, but not yet by which row. So check it.

One replay is not a refusal. A session that expired unpaid is superseded. From a few minutes after
its expiry, a POST with the same key returns a normal success with a fresh session (a new
transaction id, the same key). So a customer who comes back late can just pay. Keep the
order-derived key for the life of the order to keep that path open. The band ends about an hour
after the expiry, when the platform closes the attempt. Then the replay answers
`PRIOR_ATTEMPT_FAILED` and the key is spent: reconcile with `getStatus` and use a fresh key.

**Cart changed = new session, new idempotency key.** There is no call that updates a session. If
the amount or the contents of the order change after a session exists, abandon it and create a
fresh session with a fresh key.

## Sessions expire

A session is valid for about 2 hours. If the payer comes back later, create a new one. Before you
show the widget again for a stored session, read the status. The widget of a completed session
shows "session is closed or expired", which looks like an error to someone who just paid.

## Card fields

Card fields put the card form in your own page instead of the hosted widget. We enable them for
each merchant on request: ask Dominaite support. On an account without them, `FIELDS` throws
`ApiException` with HTTP status 400 and code `INVALID_SELECTION`.

```java
CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
    .amount(2500)
    .currency("EUR")
    .orderReference("order-1042")
    .integration(CheckoutIntegrations.FIELDS) // null = CheckoutIntegrations.WIDGET
    .idempotencyKey(IdempotencyKeys.forOrder("checkout", "order-1042", 2500, "EUR"))
    .build());
```

`session.getIntegration()` echoes what the session was created for. A fields session also has
`getClientSecret()`, the browser credential for that one session. Give the transaction id, the
integration, the cashier key, the cashier token and the client secret to the payment page. Escape
them like any value in a template, and never log the secret. The integration is part of the
idempotency identity: the same key with a different integration is `IDEMPOTENCY_KEY_REUSED`.

Browser events are for the screen of the payer only. Mark the order paid only from the
`payment.succeeded` webhook or a `getStatus` read, the same as with the widget.

## Stored payment methods (recurring)

A session can ask the payer to save their card. Set `saveCard(true)` on the request. Nothing else
about the session changes. A request that never sets it sends the same bytes as before (null is
left out, not sent as `false`).

When that session is paid, `getStatus` has a `getStoredPaymentMethod()`: an id, the brand, the last
four digits, the expiry and a status (`active`, `revoked`, `expired` or `retired`). `retired` means
that the platform stopped the card on its own. `getRetiredReason()` says why (`hard_decline`,
`chargeback` or `source_sale_reversed`). Keep the id with your customer. The full card number never
reaches the SDK. The brand, the last four and the expiry can be null.

```java
CheckoutStatus status = client.getStatus(session.getTransactionId());
StoredPaymentMethod method = status.getStoredPaymentMethod();
if (method != null && method.isChargeable()) {
    storeForCustomer(customerId, method.getId()); // pm_...
}
```

Charge the stored card later, without the payer, with `chargePaymentMethod`. It takes the same
amount, currency and order reference as a session, and a required idempotency key. Send the same
key when you retry.

We switch stored-card charges on for each account. Until then `chargePaymentMethod` throws
`ChargeException` with code `PAYMENT_METHOD_CHARGES_DISABLED` (HTTP 503). We also send this code
when we pause charges for a short time. If we never switched charges on for your account, a retry
does not help: ask Dominaite support. If they are on, retry later with the same key.

```java
try {
    PaymentMethodCharge charge = client.chargePaymentMethod(methodId, ChargeRequest.builder()
        .amount(2500)
        .currency("EUR")
        .orderReference("order-1043")
        .description("Monthly plan")
        .idempotencyKey(IdempotencyKeys.forOrder("charge", "order-1043", 2500, "EUR"))
        .build());

    if (charge.isPaid()) {
        markPaid(charge.getTransactionId());
    } else if (ChargeStatuses.PENDING.equals(charge.getStatus())) {
        pollLater(charge.getTransactionId()); // not terminal
    } else if (DeclineClasses.HARD.equals(charge.getDeclineClass())) {
        stopCharging(methodId); // never retry
    } else if (DeclineClasses.SOFT_SCA_REQUIRED.equals(charge.getDeclineClass())) {
        bringThePayerBack(); // needs a session
    }
} catch (ChargeException error) {
    if (ErrorCodes.CHARGE_OUTCOME_UNKNOWN.equals(error.getCode())) {
        // The provider gave no verdict: the charge MAY have happened.
        // Poll the transaction or wait for the webhook. Never retry under a new key.
        pollLater(error.getTransactionId());
    } else if (error.isRetryable() || ErrorCodes.DUPLICATE_REQUEST.equals(error.getCode())) {
        retryLater(error.getIdempotencyKey()); // nothing was charged: the SAME key
    } else {
        throw error;
    }
}
```

`chargePaymentMethod` returns for HTTP 201 and for HTTP 402. **A decline is a result, not an
exception.** The 402 charge has the status `failed`, the decline code of the provider and a decline
class (`hard`, `soft_funds`, `soft_sca_required` or `soft_other`). The decline class can be null,
for example when nobody declined the payment. `isPaid()` and `isTerminal()` read the same way as on
a session status. `pending` is the one status that is not terminal.

`ChargeException` is the gateway answering with an error code instead of a charge: HTTP 409
(`PAYMENT_METHOD_NOT_ACTIVE`, `DUPLICATE_REQUEST`), 422 (`IDEMPOTENCY_KEY_REUSED`), 502
(`CHARGE_OUTCOME_UNKNOWN`, `CHARGE_FAILED`) or 503 (`PAYMENT_METHOD_CHARGES_DISABLED`,
`PAYMENT_PROCESSING_UNAVAILABLE`). It keeps the HTTP status, the code, the message of the gateway,
the idempotency key to reuse, and `getCharge()` when the gateway attached the charge. A 404 is a
method id that is not yours. It stays a plain `ApiException` with code `PAYMENT_METHOD_NOT_FOUND`.

`revokePaymentMethod` drops the card. The gateway deletes the saved credential at the provider and
marks the method `revoked`. A later charge on it is refused with `PAYMENT_METHOD_NOT_ACTIVE`. It is
a signed `DELETE` with an empty key and an empty body, and it returns on HTTP 204, also for a method
that is already revoked. When the gateway refuses, nothing changed, and you get `RevokeException`:
`MERCHANT_API_UNAVAILABLE` (503, retry later) or `UPSTREAM_CONTRACT_ERROR` (502, contact support with
the id).

## Verify a card without a payment

A verify-only session saves the card of the payer and takes no money. Set `verifyOnly(true)` and
leave the amount at 0. The platform sets the verification amount, and the SDK refuses any other
amount before it sends.

```java
CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
    .currency("EUR")
    .orderReference("card-check-42")
    .verifyOnly(true)
    .idempotencyKey("verify-card-check-42-EUR") // forOrder needs an amount above 0
    .build());
```

Verify-only is off by default. We switch it on for your account. Until then the create call throws
`PermissionException` with code `VERIFY_ONLY_NOT_ENABLED`.

The status read has `getVerifyOnly()` true. The status stays the plain transaction status:
`requires_capture` when the card is checked, then `cancelled` when the authorization is released. A
stored payment method that is not null means that the card is verified. The session sends
`verification.succeeded` or `verification.failed`, never `payment.*` events.

## Subscriptions

A plan is a price that you bill every period. An agreement subscribes one of your customers to a
plan. With scheduled charges on, the platform charges the stored card every period. These calls
need a payments key.

1. Create a plan with `createPlan`. Its amount, currency and interval cannot change later.
2. Create an agreement with `createAgreement`. It starts `pending`, and nothing is charged.
3. Create a checkout session with `agreementId` and `consent`. The paid session saves the card and
   activates the agreement.
4. Follow the agreement with the `agreement.*` and `charge.*` webhooks.

```java
Plan plan = client.createPlan(CreatePlanRequest.builder()
    .name("Pro monthly")
    .amount(2500)
    .currency("EUR")
    .intervalUnit(IntervalUnits.MONTH)
    .idempotencyKey("plan-pro-monthly-2500-EUR")
    .build());

Agreement agreement = client.createAgreement(CreateAgreementRequest.builder()
    .planId(plan.getId())
    .customerReference("cust-42")
    .idempotencyKey("subscription-cust-42-pro")
    .build());

IntegrationContract contract = client.getIntegrationContract();
CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
    .amount(2500)
    .currency("EUR")
    .orderReference("sub-42-1")
    .agreementId(agreement.getId())
    .consent(AgreementConsent.builder()
        .textVersion(contract.getRecurring().getConsentTextVersions().get(0)) // the wording your page shows
        .acceptedAt(acceptedAt)
        .ip(payerIp)
        .build())
    .idempotencyKey(IdempotencyKeys.forOrder("checkout", "sub-42-1", 2500, "EUR"))
    .build());
```

Subscription sessions are off until we switch them on for your account. Until then you can create
plans and agreements, but the subscription session throws `RefusalException` with code
`SUBSCRIPTIONS_NOT_ENABLED`. The agreement stays `pending`. Scheduled charges are a second switch.
Without it the platform runs no scheduled charge and no retry. Ask Dominaite support for both.

`listPlans`, `getPlan`, `updatePlan` and `deletePlan` manage your plans. `updatePlan` changes only
the name, the trial days and the cancel page. It is a `PATCH`, and it refuses a request that changes
nothing. `deletePlan` archives the plan: it takes no new agreements, and the agreements on it
continue. `getAgreement` reads an agreement, and `cancelAgreement` cancels it. The platform never
cancels an agreement on its own. A refused call throws `ApiException` with a code, for example
`PLAN_NOT_FOUND`, `PLAN_NOT_ACTIVE` or `AGREEMENT_ENDED`.

## Webhooks

Webhooks tell you that a payment succeeded without a poll. Add an endpoint for your server on the
**Webhooks** tab of the dashboard, select the events, and store the `whsec_...` secret that it
shows. The dashboard shows the secret once. A new secret stops the old one. You can also register
endpoints from your server: see [Webhook endpoints](#webhook-endpoints).

**Verify the signature before you parse the body.** An unverified webhook is a stranger that sends
JSON to your server.

```java
// body must be the RAW request bytes, exactly as received.
try {
    WebhookEvent event = Webhooks.verifyAndParse(body, signatureHeader, secret);
    // Dedupe on event.getId(), queue the work, then answer 2xx.
} catch (WebhookVerificationException error) {
    if (error.getReason() == WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE) {
        // A replay, or your clock drifted.
    }
    // Answer 400.
}
```

`Webhooks.verify` checks the delivery only. `Webhooks.verifyAndParse` verifies first, and only then
parses the body into a `WebhookEvent`: `getId()`, `getType()`, `getApiVersion()`, `getCreatedAt()`,
`getData()` (the raw `data` object), and typed data. `getPayment()` is set for `payment.*` and
`verification.*` events, `getAgreement()` for `agreement.*` events and `getCharge()` for `charge.*`
events. A body that verifies but is not a readable envelope throws with
`WebhookFailureReason.MALFORMED_PAYLOAD`. Both methods have overloads for a `byte[]` and a `String`
body, and for your own tolerance and clock: `(payload, signatureHeader, secret, toleranceSeconds,
nowUnixSeconds)`.

```java
WebhookEvent event = Webhooks.verifyAndParse(body, signatureHeader, secret);
if (WebhookEventTypes.PAYMENT_SUCCEEDED.equals(event.getType()) && event.getPayment() != null) {
    fulfil(event.getPayment().getOrderReference(), event.getPayment().getTransactionId());
}
```

The MAC comparison is constant-time (`MessageDigest.isEqual`). It runs before the timestamp check,
so an unsigned request learns nothing about your tolerance window.

The signature arrives in `X-Webhook-Signature` as `t={unix_seconds},v1={lowercase_hex}`. It is an
HMAC-SHA256 over `"{t}.{raw_body}"` with the UTF-8 bytes of your `whsec_` secret as the key. The
default tolerance is 300 seconds, the same as the server.

Frameworks often get the raw body wrong. If your handler gives you a deserialized object and you
serialize it again to verify, the property order or the whitespace changes, and every delivery
fails as `SIGNATURE_MISMATCH`. In Spring, take the body as `@RequestBody byte[]`. In a servlet,
read `request.getInputStream()` before any JSON layer touches it.

### The envelope

The envelope is flat JSON with no `success` wrapper.

```json
{
  "id": "<delivery id - your dedupe key>",
  "type": "payment.succeeded",
  "apiVersion": "2026-09-25",
  "createdAt": "<ISO 8601 UTC instant of the transition>",
  "data": {
    "transactionId": "...",
    "orderReference": "order-123",
    "status": "succeeded",
    "previousStatus": "pending",
    "kind": "sale",
    "amount": 8440,
    "grossAmount": 8701,
    "surchargeAmount": 261,
    "currency": "EUR",
    "originalTransactionId": null,
    "idempotencyKey": "order-123"
  }
}
```

Amounts are minor units. On `payment.*` events, `amount` is what you are PAID (the base), and
`grossAmount` is the card movement. On `payment.refunded`, `amount` is what went back to the
customer. `getOrderReference()` is your own order reference: match events to your orders on it.
Every field except the transaction id can be null. Fields that `PaymentEventData` does not model
yet stay on `getRaw()`.

### Events

`payment.succeeded`, `payment.failed`, `payment.requires_capture`, `payment.cancelled`,
`payment.abandoned`, `payment.refunded`, `payment.disputed`. For stored-card charges and
subscriptions: `charge.succeeded`, `charge.retrying`, `charge.failed`, `agreement.activated`,
`agreement.past_due`, `agreement.cancelled`. For verify-only sessions: `verification.succeeded`,
`verification.failed`. The constants are on `WebhookEventTypes`.

`payment.succeeded` is the only signal that means the money is in hand. On `payment.failed` and
`verification.failed`, `getDeclineClass()` says why the payment failed. It can be null.

`payment.*` data carries `getStoredPaymentMethod()`, the same object as on the status read. It is
set when the card was stored together with the approval. It can be null when a card WAS saved, so on
a `saveCard` session whose webhook has none, read it with `getStatus`.

### Ordering

Deliveries can arrive out of order. Keep the highest sequence that you processed for each object,
and discard an event whose sequence is not higher. `getOrderingKey()` on `AgreementEventData` and
`ChargeEventData` gives the object key: the agreement id, the `agreementId/periodNumber` of a
scheduled charge, or the charge id of a one-off charge. `getSequence()` is null on a delivery from a
server that predates the field. Treat it as 0. `payment.*` events have no sequence: dedupe them on
the id, and read the status when the order matters.

### Delivery

Delivery is **at least once**, so the same event can arrive twice. Dedupe on the id. Answer 2xx
quickly and queue the work. Failed deliveries are retried. Webhooks do not replace your
reconciliation sweep: keep a sweep that reads the status of orders you think are unpaid.

### Webhook endpoints

You can register endpoints from your server too. These calls need a reporting key with the
`webhooks:manage` scope. A payments key never works here.

```java
WebhookEndpoint endpoint = client.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
    .url("https://shop.example.com/webhooks/dominaite")
    .events(List.of(WebhookEventTypes.PAYMENT_SUCCEEDED, WebhookEventTypes.PAYMENT_REFUNDED))
    .idempotencyKey("webhook-orders-endpoint")
    .build());
storeWebhookSecret(endpoint.getSecret()); // whsec_...
```

Store the secret at once. A replay of a create that already succeeded answers without it, so the
client never retries this call. After a timeout or another `TransportException`, retry with the
same key. If the answer has no secret, delete that endpoint and create it again with a new key.
`listWebhookEndpoints` lists your endpoints, and `deleteWebhookEndpoint` removes one.

## Status polling

Use this when you cannot receive webhooks, and as the read side of the reconciliation sweep.

```java
CheckoutStatus status = client.getStatus(session.getTransactionId());
if (status.isPaid()) {
    // fulfil the order
}
```

The status is one of `pending`, `processing`, `succeeded`, `failed`, `refunded`,
`partially_refunded`, `cancelled`, `disputed`, `requires_capture` or `abandoned`. These are the
`TransactionStatuses` constants, and `TransactionStatuses.ALL` lists them. **`succeeded` is the only
value that means the customer paid**, and `isPaid()` checks it. `isTerminal()` tells you when to
stop polling. It reports an unknown status as NOT terminal, so a value that the API adds later keeps
you polling and does not close an open order.

`requires_capture` is **not** unpaid: the payer paid and the funds are held until capture. `disputed`
is not terminal either: a chargeback is open.

`getPaymentMethod()` says how the payer paid (`PaymentMethodCategories`: `card`, `wallet`,
`bank_transfer`, `sepa`). `getWalletType()` says which wallet (`WalletTypes`: `apple_pay`,
`google_pay`, `samsung_pay`). Both are null while the payment is open.

Call this from your server, never from the browser. Poll when the payer comes back or on your order
timeout, not in a tight loop: the endpoint has a rate limit for each key.

## Fields the SDK does not model yet

A new API field never needs a new SDK release:

- Every response type has `getRaw()`, the unparsed JSON as a Jackson `JsonNode`. Read a new field
  there.
- Every request type with a body has `extra(name, value)` on its builder. The SDK adds each entry
  to the JSON body at the top level. It ignores a name that is already a modelled field.
  `ListTransactionsRequest.Builder.extra` adds query parameters the same way.
- Statuses, decline classes and event types are plain strings. A value that the SDK does not know
  is valid, and the SDK never throws on it.

## Errors

Every exception is unchecked and extends `DominaiteException`. Catch the subclass, and read
`getCode()` for the machine-readable code when there is one. `getHttpStatus()` has the HTTP status,
`getRequestId()` has the request id for support, and `getIdempotencyKey()` has the key to reuse.
The constants of every code are on `ErrorCodes`, with one list per route: `CHARGE`, `REFUND`,
`REVOKE`, `RECURRING` (plans and agreements), `WEBHOOK` and `SESSION_REFUSALS`.

The request id is the `Dominaite-Request-Id` response header when the response has one, else
`metadata.requestId` in the body. Every result type has `getRequestId()` too. It is null when no
response arrived.

| Exception | When | What to do |
|---|---|---|
| `RefusalException` | HTTP 200 with `success: false` on a session create. Has `getTransactionId()` and `getRawResult()`. | Branch on the code. Do not blind-retry. |
| `StorefrontException` | The session create was refused because of the storefront: `STOREFRONT_NOT_WHITELISTED` or `STOREFRONT_INACTIVE` (409), `STOREFRONT_MISMATCH` (400, or 200 on a replay). | Fix the configuration. A retry does not help. |
| `AuthenticationException` | 401. The code is `INVALID_API_KEY`, `INVALID_SIGNATURE`, `TIMESTAMP_OUT_OF_RANGE` or `IP_NOT_ALLOWED`. | Fix the key id, the secret, the server clock or the allowlist. Never retry in a loop. |
| `PermissionException` | 403. The key and the signature were good, but the call is not allowed. `VERIFY_ONLY_NOT_ENABLED` arrives as this type. | For verify-only, contact Dominaite support. Never retry in a loop. |
| `InsufficientScopeException` | 403 `INSUFFICIENT_SCOPE`. A subclass of `PermissionException`. The message names the scope. | Use a key that has the scope. |
| `RateLimitedException` | 429. A limit refused the call and nothing was done. The code is `RATE_LIMIT_EXCEEDED`, `CHARGE_VELOCITY_EXCEEDED` or `CHARGE_DECLINE_RATE_EXCEEDED`. `getRetryAfter()` is the `Retry-After` wait as a `Duration`, or null. | Wait `getRetryAfter()` (a few seconds when null), then retry with the **same** idempotency key. `isRetryable()` is false, and the client never retries it. |
| `TransportException` | A network failure, a timeout or a 5xx without a code of the route, also one whose body is an HTML page from a proxy. A coded 5xx keeps its code, such as `INTERNAL_ERROR`. On a call the client retries, it already did. | Retry later with the **same** idempotency key. `isRetryable()` is always true. |
| `ChargeException` | `chargePaymentMethod` got an error code instead of a charge, also a 404 `PAYMENT_METHOD_NOT_FOUND` or `AGREEMENT_NOT_FOUND`. | See [Stored payment methods](#stored-payment-methods-recurring). |
| `RefundException` | A refund call got an error code instead of a refund. | See [Refunds](#refunds). |
| `RevokeException` | `revokePaymentMethod` was refused: 502 or 503. Nothing changed. | Retry later on 503. Contact support on 502. |
| `ApiException` | Any other refusal or unexpected answer, also a 3xx. `getRawResult()` has the whole envelope: the field errors of a 400 are in `error.validationErrors`. | Read the status and the code. A 404 from `getStatus` is an unknown transaction id. |
| `ValidationException` | A bad argument: an amount that is not positive, a missing field, a missing idempotency key, a malformed key id. | Fix the call. Nothing was sent. |
| `WebhookVerificationException` | A webhook delivery failed verification. `getReason()` says why. | Answer 400. Do not process the body. |

`RefusalException`, `StorefrontException`, `ChargeException`, `RefundException` and
`RevokeException` extend `ApiException`.

Refusal codes on `RefusalException`, with constants on `ErrorCodes`:

- `PAYMENT_PROCESSING_UNAVAILABLE`: card payments are off now and nothing was created. Retry with
  the same key. `isRetryable()` is true, and the client retries it for you.
- `DUPLICATE_REQUEST`: an attempt with this key is unfinished, but its session cannot be given back
  now. Send the same key again shortly, never a fresh one.
- `ALREADY_PROCESSED`: the payment of this key already completed.
- `PRIOR_ATTEMPT_FAILED`: the earlier attempt with this key failed. Use a fresh key.
- `IDEMPOTENCY_KEY_REUSED`: the same key with a different body. Use a fresh key.

## Refunds

`createRefund` refunds a payment that you took through a checkout session or a charge, in full or
in part. The amount is in minor units of the currency of the payment. The idempotency key is
required. Derive it from YOUR refund (the return or credit-note id), and send the same key when you
retry. The same key names the same refund and never refunds twice.

```java
Refund refund = client.createRefund(transactionId, RefundRequest.builder()
    .amount(MinorUnits.from(new BigDecimal("1500"), "HUF")) // 1500: HUF has 0 decimals on the gateway
    .reason("Returned item")
    .idempotencyKey(IdempotencyKeys.forOrder("refund", "rma-77", 1500, "HUF"))
    .build());
// refund.getStatus() is "pending": queued, not done yet.
```

Leave the amount null to refund everything that is still refundable. The SDK then sends no
`amount`. The reason is optional free text, at most 500 characters.

The call answers when the refund is queued (HTTP 202). Read the result with `getRefund`, or wait for
`payment.refunded`:

```java
Refund current = client.getRefund(transactionId, refund.getRefundId());
if (current.isTerminal() && !current.isSucceeded()) {
    // current.getFailureCode() says why. A new attempt needs a new idempotency key.
}
```

The status is `pending`, `processing`, `succeeded` or `failed` (`RefundStatuses`). Only `succeeded`
and `failed` are final. A failed refund is a result, not an exception: `getFailureCode()` is
`REFUND_AMOUNT_EXCEEDED`, `PAYMENT_NOT_REFUNDABLE` or `REFUND_FAILED`. Treat any other value as
`REFUND_FAILED`. A failed refund sends no webhook.

`listRefunds` lists the refunds of one payment, newest first. It also has `getRefundedAmount()` and
`getRefundableAmount()`. Read the refundable amount before a partial refund.

A refused refund call throws `RefundException`. `isRetryable()` is true for `DUPLICATE_REQUEST`
(send the SAME key again after a second) and `REFUND_NOT_FOUND` (right after the create call, poll
again for up to 60 seconds). The other codes are `PAYMENT_NOT_FOUND`, `PAYMENT_NOT_REFUNDABLE`,
`REFUND_AMOUNT_EXCEEDED`, `IDEMPOTENCY_KEY_REUSED` and `IDEMPOTENCY_KEY_REQUIRED`. A 5xx means that
nothing was queued: it is a `TransportException`, so retry with the same key.

## Transactions

`listTransactions` lists your online payments in a time window, oldest first, one page at a time.
It needs a reporting key with the `transactions:read` scope. A payments key never works here.
`from` is inclusive, `to` is exclusive, and the window is at most 31 days.

```java
ListTransactionsRequest request = ListTransactionsRequest.builder()
    .from(Instant.parse("2026-10-01T00:00:00Z"))
    .to(Instant.parse("2026-11-01T00:00:00Z"))
    .build();
while (true) {
    TransactionPage page = client.listTransactions(request);
    for (TransactionItem item : page.getItems()) {
        book(item);
    }
    if (page.getNextCursor() == null) {
        break; // the last page
    }
    request = request.withCursor(page.getNextCursor());
}
```

Each item has the transaction id, your order reference, the type (`payment`, `charge` or
`verification`), the status, the amount, the currency and the refunded amount. The status and the
amount are the same as on the status read. A `verification` item is a card check, not a sale: leave
it out of your revenue.

### Settlement

`getFee()`, `getNetAmount()`, `getSettledAt()` and `getPayoutId()` are null until the acquirer
settles the payment. The fee usually arrives on the next business day (T+1). The fee and the net
amount are in minor units, like the amount. **The net amount is the amount minus the fee of this
payment only.** Deductions on the whole payout are not in it. The fee and the net amount can change
after they appear, so read a settlement day again before you close your books on it.

To get one settlement day, set `settledFrom` to that day and `settledTo` to the next day. Both are
`LocalDate` values, sent as `yyyy-MM-dd`. Set both or neither. `from` and `to` still apply on the
creation time, so keep them wide enough to cover when those payments were created, for example the
last 31 days:

```java
LocalDate today = LocalDate.now(ZoneOffset.UTC);
Instant now = Instant.now();
TransactionPage page = client.listTransactions(ListTransactionsRequest.builder()
    .from(now.minus(Duration.ofDays(31)))
    .to(now)
    .settledFrom(today.minusDays(1))
    .settledTo(today)
    .build());
```

The SDK sends the filters in the query string. The query string is not signed: the signed path is
`/merchant-api/transactions`.

## Integration contract

`getIntegrationContract()` reads the public contract of the API that the client calls. It has the
contract version, the status and event values, and the consent wording versions for subscription
sessions. `getRaw()` has the whole contract.

## The three identifiers

- `transactionId`: the Dominaite payment id. Store it, and poll the status with it.
- `orderReference`: your own id, echoed back. You search for it in your dashboard, so put your
  order or cart id there.
- `orderId` (`dom_...`): the correlation id for the provider. You never need it.

## License

MIT. See [LICENSE](LICENSE).

## Versioning and support

The SDK is 0.x. A minor release (for example 0.6 to 0.7) can contain a breaking change.
[CHANGELOG.md](CHANGELOG.md) lists every breaking change under "Breaking", with what to change in
your code. Pin an exact version and read the CHANGELOG before you upgrade.

The SDK supports Java 11 and later. CI tests it on JDK 11, 17 and 21.

The API can tell you that your SDK version is old. It sends the `Dominaite-Sdk-Notice` response
header, and the SDK passes it on as a warning once for each distinct notice (see
[Logging and warnings](#logging-and-warnings)). Send the warnings to your logs, and upgrade when
you see one. The header is optional: a response without it gives no notice.
