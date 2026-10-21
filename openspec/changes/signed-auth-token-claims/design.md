# Design

## Context

See [proposal.md](proposal.md) for motivation and [specs/auth-token/spec.md](specs/auth-token/spec.md) for the required behavior. The current Quarkus 3.27.4 / Java 21 service authenticates in `AuthResource`; `LegacyTokenUtil` Base64-encodes `username|role|exp|secret`. The source has no signature, no token verifier, and no `AUTH_TOKEN_SECRET` configuration. The Maven dependencies do not currently include a JWT library, and the only auth test covers a successful login with a non-blank `client`.

The Issue describes the current token as HMAC-based, which differs from the inspected code and the Day 1 endpoint contract. This design treats the checked-in behavior and contract as the baseline: the existing token is not signed. No token consumer or verification path was found in this repository.

## Goals / Non-Goals

**Goals:**
- Replace the custom Base64 payload with a standard signed token while keeping the login response envelope stable.
- Make token issuance and verification share one explicit set of claims, signing key, and expiration semantics.
- Keep the change bounded to login/token behavior, its configuration, and focused tests.

**Non-Goals:**
- Redesign credential storage, database access, or the overall authentication flow.
- Add a login-adjacent endpoint, refresh tokens, revocation storage, authorization policy, or new identity claims beyond the spec.
- Change health behavior, CORS, or the documented 500 response details in this change.

## Protected Contracts

- For a successful login, preserve `200 OK`, the string `token`, integer `expiresIn` of 3600, `user.username`, escaped `user.displayName`, `user.role`, and `debug`.
- Preserve the documented 400 responses for a null request or missing/blank username or password, the 401 response for invalid credentials, and the 500 response shape for an unexpected failure. Add the spec's 400 response for absent/blank `client`.
- Keep `POST /auth/login` unauthenticated and JSON-based. The response field `token` remains a string, but its contents intentionally stop being the legacy Base64 format; clients must treat it as opaque.
- Do not change `GET /q/health` or introduce a new externally visible token-verification endpoint.

## Components Affected

- **Login resource:** validates the required request fields, coordinates credential authentication and token issuance, and retains the response shape.
- **Token boundary:** replace the legacy token encoding with issuance and verification through one focused token component. Verification remains an internal capability; it does not imply a new route.
- **Runtime configuration:** provide the signing key through `AUTH_TOKEN_SECRET`; do not put a production key in source control or a default in `application.properties`.
- **Tests:** characterize the current endpoint before changing critical behavior, then cover the new token contract at unit and HTTP integration boundaries.

## Decisions

### Use a standard JWT/JWS representation signed with HS256

JWT/JWS is the token format; HMAC-SHA-256 (HS256) is the signing algorithm. Use a maintained JWT library compatible with the repository's Quarkus version rather than implementing token serialization, signing, or verification locally. The issued token carries only the required `sub`, `client`, `iat`, and `exp` claims; it is signed, not encrypted, so no secret or other confidential data belongs in its payload.

Alternatives considered:
- **Custom HMAC envelope:** rejected because canonicalization, parsing, claim validation, and signature checks would become application-owned security code.
- **Asymmetric JWT signing:** not selected for this single-service scope because no independent verifier or key-distribution requirement was found. It remains preferable if separate services must verify tokens without receiving signing authority.
- **Continue Base64 encoding:** rejected because it provides neither integrity nor confidentiality and embeds the current hardcoded secret.

### Require an externally supplied signing key

Read `AUTH_TOKEN_SECRET` from runtime configuration. Production startup must fail when it is absent or invalid; do not silently fall back to the source-code secret. Local and test runs must provide an explicit non-production key. For HS256, key material must meet the algorithm's minimum 256-bit strength. Key rotation invalidates tokens signed with the previous key; the operational procedure must account for the 3600-second maximum token lifetime.

### Derive all token times from one issuance instant

Capture one issuance instant at whole-second precision; set `iat` to that instant and `exp` to 3600 seconds after it. Keep the documented `expiresIn` value at 3600. Document a one-second comparison tolerance for `expiresIn` versus `exp - now` to account for integer-second boundaries; tests must use a controlled clock where possible. Reject a token when verification time is at or beyond `exp`.

### Preserve the accepted client value without inventing an allowlist

Reject a missing, null, empty, or whitespace-only `client` with 400. For other strings, retain the request value exactly in the claim; do not trim or normalize it, since the spec requires the claim to equal the accepted request value. Do not use `client` as an authorization decision in this change.

## Incremental Strategy

1. Add characterization coverage for the existing login contract before changing token or validation behavior.
2. Establish the token-library and external-key configuration boundary, with isolated tests for claims, signature verification, tampering, expiry, and key failure modes.
3. Integrate issuance with login and require `client`, retaining the existing success response fields and the existing username/password and credential error behavior.
4. Add HTTP-level checks for the new 400 case, response compatibility, and 3600-second expiry reporting; verify that logs do not contain tokens or key material.
5. Validate the configured key and client rollout in a non-production environment before deployment. Keep the response field `token` opaque to consumers; coordinate any consumer that currently decodes the legacy payload.

## Characterization Tests Before Critical Changes

Before changing token generation or login validation, record the current behavior in tests:
- Successful login returns 200 and the documented response keys, nested user fields, escaped display name, `debug` value, and integer `expiresIn` of 3600.
- Null request, missing/blank username, missing/blank password, and invalid credentials retain their documented status and response messages.
- A controlled unexpected failure retains the documented 500 response shape. Do not assert or log sensitive exception details beyond the existing contract.
- Characterize the legacy token's Base64 payload structure and lifetime only to make the intentional format break explicit; never print or snapshot the embedded secret.

After the switch, replace legacy-format assertions with tests that inspect the required claims and verify signature, tampering rejection, expiry boundaries, `expiresIn` tolerance, absent/blank `client`, external key configuration, and log redaction. These tests must not emit token or key values in failure output.

## Risks / Trade-offs

- **Legacy clients may decode the old Base64 token** -> Keep the response envelope stable, treat `token` as opaque in documentation, and coordinate client rollout; no silent dual-format acceptance is planned.
- **A shared HS256 key grants both signing and verification authority** -> Restrict key access to the service and use a sufficiently strong injected key; revisit asymmetric signing if independent verifiers emerge.
- **Key rotation invalidates active tokens** -> Coordinate rotation and deployment around the 3600-second lifetime; require re-login rather than adding token migration state.
- **The spec does not define an allowlist for `client`** -> Treat the claim as informational only; do not make authorization decisions from it.
- **The baseline 500 response exposes exception details** -> Preserve it for compatibility as required by the current spec, but track its remediation separately; ensure token and key values are not included in logs or error responses.
- **A JWT library adds a dependency and attack surface** -> Select a maintained Quarkus-compatible release, keep validation strict, and avoid custom parsing or algorithm fallback.

## Rollback

There is no persistent token data to migrate. If the release must be rolled back, restore the previous application version and have clients re-authenticate; tokens issued after the format switch may not be understood by consumers that decode the legacy payload. Avoid mixed-format fallback in the new verifier because it would re-accept unsigned tokens. Rollback restores the hardcoded-secret behavior, so treat it as a temporary compatibility action, preserve `AUTH_TOKEN_SECRET` only for the new version, and redeploy the corrected release promptly.

## Open Questions

- The exact Maven artifact and version for JWT/JWS support can be selected during implementation after checking compatibility with Quarkus 3.27.4. This does not change the selected JWT/JWS + HS256 approach or the contracts above.