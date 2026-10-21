# Tasks

## 1. Characterize current behavior before functional changes

- [x] 1.1 Expand the successful-login characterization in `AuthResourceTest` to assert status, response keys, nested user fields, escaped display name, `debug`, and integer `expiresIn` of 3600; verify with `mvn -Dtest=AuthResourceTest test`.
- [x] 1.2 Characterize current 400/401 behavior for null request, missing or blank username/password, and invalid credentials, plus the current acceptance of omitted `client`; verify each documented status and message with `mvn -Dtest=AuthResourceTest test`.
- [x] 1.3 Characterize the documented 500 response shape through a controlled resource-level failure without adding a production test hook; verify `message` and `exception` are returned with status 500.
- [x] 1.4 Characterize the legacy token payload and 3600-second lifetime without snapshotting or printing the embedded secret or complete token; verify the test output contains no credential material.

## 2. Establish signed-token and key configuration

- [x] 2.1 Select a maintained JWT/JWS library compatible with Quarkus 3.27.4, add its dependency, and verify Maven resolves the dependency tree without version conflicts.
- [x] 2.2 Configure `AUTH_TOKEN_SECRET` as externally supplied key material, reject missing or weak production configuration without a fallback, and provide an explicit non-production test key; verify startup/configuration tests cover each case.
- [x] 2.3 Document the `AUTH_TOKEN_SECRET` source, minimum strength, missing-key behavior, and laboratory-only handling of any retained default; verify the documentation contains no real key value.
- [x] 2.4 Implement signed JWT/JWS issuance with HS256 and the `sub`, `client`, `iat`, and `exp` claims; verify unit tests assert the claims and validate a freshly issued token's signature.
- [x] 2.5 Implement internal verification rejection for modified contents, invalid signatures, and tokens at or beyond `exp`, using a controlled clock for boundary cases; verify focused token tests pass without adding a public endpoint.

## 3. Integrate login behavior and preserve the response contract

- [x] 3.1 Require `client` to be a non-blank string, preserve its accepted value in the token claim, and return 400 when absent, null, empty, or whitespace-only; verify HTTP tests cover all cases without introducing an allowlist.
- [x] 3.2 Integrate token issuance into `POST /auth/login` while preserving 200, `token`, `expiresIn`, `user`, and `debug`; verify the response fields and `expiresIn`/`exp - now` coherence within the documented one-second tolerance.
- [x] 3.3 Verify login and token verification logs contain neither issued tokens nor signing-key material; use captured logs in tests and ensure assertion output does not print sensitive values.
- [x] 3.4 Update `docs/ENDPOINTS.md` to mark `client` required, describe `token` as opaque and signed with the required claims, and document the expiration behavior; verify the documented success and existing error contracts match the specs.

## 4. Integration verification

- [x] 4.1 Run `mvn verify` and confirm characterization, token, login, configuration, and logging tests pass together with no changes to `GET /q/health` or unrelated behavior.