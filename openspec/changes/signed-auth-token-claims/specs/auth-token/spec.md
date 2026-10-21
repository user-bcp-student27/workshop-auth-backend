# Spec Delta

## Purpose

Define el comportamiento observable de los tokens emitidos durante el login, incluidos sus claims, expiracion y proteccion de datos sensibles, preservando el contrato documentado de autenticacion salvo los cambios comprometidos por la Issue #12.

## ADDED Requirements

### Requirement: The login response preserves the documented contract
For valid credentials, the system MUST preserve the documented success status and response fields, except for the explicitly changed token semantics and the new required `client` input. It MUST preserve the documented authentication error statuses and response shapes for other login outcomes.

#### Scenario: Successful login keeps the response fields
- **WHEN** a user submits valid credentials and a non-blank `client`
- **THEN** `POST /auth/login` returns `200 OK` with `token`, integer `expiresIn`, `user.username`, `user.displayName`, `user.role`, and `debug`
- **AND** `expiresIn` remains 3600 seconds

#### Scenario: Missing username or password keeps the documented client error
- **WHEN** the request is null, or `username` or `password` is null, empty, or blank
- **THEN** `POST /auth/login` returns `400 Bad Request` with the corresponding documented `message`

#### Scenario: Invalid credentials keep the documented unauthorized response
- **WHEN** the request has valid required fields but the credentials do not match a user
- **THEN** `POST /auth/login` returns `401 Unauthorized` with the documented `message` field

#### Scenario: Unexpected login failure keeps the documented server error shape
- **WHEN** an unhandled failure occurs during login
- **THEN** `POST /auth/login` returns `500 Internal Server Error` with `message` and `exception` fields

### Requirement: Client is required and represented in the token
The system MUST require `client` as a non-blank string for login and MUST place the accepted request value in the issued token's `client` claim. This change does not define an allowlist or additional syntax rules for non-blank values.

#### Scenario: Client is absent or blank
- **WHEN** `client` is omitted, null, empty, or contains only whitespace
- **THEN** `POST /auth/login` returns `400 Bad Request`

#### Scenario: Client is accepted
- **WHEN** a user submits valid credentials and an accepted non-blank `client`
- **THEN** the issued token contains a `client` claim equal to the request value

### Requirement: Issued tokens contain the required identity and time claims
The system MUST issue a signed token containing `sub`, `client`, `iat`, and `exp`; `sub` identifies the authenticated user, and the time claims represent issuance and expiration.

#### Scenario: Valid login issues a token with minimum claims
- **WHEN** a user completes login with valid credentials and `client`
- **THEN** the token contains `sub`, `client`, `iat`, and `exp`
- **AND** `sub` identifies the authenticated user

### Requirement: Token integrity is verifiable
The system MUST allow a token's authenticity to be verified and MUST reject a token whose signed contents have been altered or whose signature is invalid.

#### Scenario: Altered token is rejected
- **WHEN** a token's contents are changed after issuance
- **THEN** token verification rejects it

#### Scenario: Invalid signature is rejected
- **WHEN** a token has an invalid signature
- **THEN** token verification rejects it

### Requirement: Token expiration agrees with the login response
The system MUST set `expiresIn` to the remaining lifetime represented by `exp - now`, within a documented tolerance, and MUST reject expired tokens.

#### Scenario: Expiration and expiresIn are coherent
- **WHEN** a successful login response is issued
- **THEN** its `expiresIn` agrees with `exp - now` within the documented tolerance

#### Scenario: Expired token is rejected
- **WHEN** token verification occurs at or after the token's `exp`
- **THEN** token verification rejects the token as expired

### Requirement: Signing secret is not exposed or used as a production default
The system MUST NOT expose the signing secret in an issued token or use an implicit default signing secret outside the laboratory. Any retained laboratory default MUST be documented as laboratory-only.

#### Scenario: Production does not use a default secret
- **WHEN** the service runs outside the laboratory
- **THEN** token signing does not use an implicit default secret

#### Scenario: Laboratory default is documented
- **WHEN** a default signing secret is retained for laboratory use
- **THEN** its configuration documentation identifies it as laboratory-only

#### Scenario: Issued token does not reveal the secret
- **WHEN** a token is issued
- **THEN** the signing secret is not present in the token contents

### Requirement: Sensitive authentication values are not logged
The system MUST NOT write issued tokens, signing secrets, or other sensitive authentication values to application logs.

#### Scenario: Login activity does not log sensitive values
- **WHEN** a login succeeds or fails
- **THEN** application logs contain neither the token nor the signing secret
- **AND** application logs contain no sensitive authentication values