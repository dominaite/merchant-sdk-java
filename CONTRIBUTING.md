# Contributing

Thank you for helping. This file lists the rules a change must follow before we can merge it.

## Run the tests

```
./mvnw -B verify
```

The full suite must pass before you open a pull request. CI runs the same command on JDK 11, 17
and 21.

## Runtime floor

The SDK supports Java 11 and later. The compiler builds with `--release 11`, so do not use an API
or a language feature that is newer than Java 11. The only runtime dependency is Jackson
databind. Do not add another runtime dependency.

## Tests

Every change gets a test. Test what a caller of the SDK gets, not how the SDK works inside. A bug
fix gets a test that fails on the old code.

## Contract fixtures

The SDK follows the public integration contract of the API
(`GET /merchant-api/integration/contract`). It does not invent rules of its own. Two fixtures in
`src/test/resources` pin the SDK to that contract:

- `merchant-api-contract.json` is maintained by hand and is identical in every Dominaite SDK.
  Do not edit it in a pull request to this repository.
- `merchant-api-wire-contract.json` is generated from the production contract by the jq
  projection in `.github/workflows/contract-drift.yml`. Never edit it by hand. When the
  contract-drift workflow fails, the API changed: regenerate the fixture with that projection,
  then update the SDK so that `ContractTest` passes.

## Versioning

The SDK is 0.x on purpose. A minor release (0.6 to 0.7) can contain a breaking change. Every
breaking change is listed under "Breaking" in [CHANGELOG.md](CHANGELOG.md). Put your change in
the CHANGELOG under the unreleased version. Do not change the version in `pom.xml`.

## Releases

The maintainer tags releases. A tag `vX.Y.Z` that matches the `pom.xml` version publishes the
package to Maven Central. Pull requests never create tags or releases.

## Security

Do not open a public issue for a security problem. See [SECURITY.md](SECURITY.md).
