# Proposal

## Why

GitHub Issue [#12](https://github.com/user-bcp-student27/workshop-auth-backend/issues/12) requiere que los tokens de login tengan integridad verificable, identidad y expiracion coherentes, y que no se expongan secretos en logs. Esto importa porque el contrato del Dia 1 describe un token Base64 que incluye el secreto y no establece claims verificables.

## What Changes

- **BREAKING**: reemplazar la semantica interna legacy del token por un token firmado con los claims `sub`, `client`, `iat` y `exp`; el campo de respuesta `token` sigue siendo un string. Un consumidor que dependiera de decodificar el formato Base64 anterior debera adaptarse.
- **BREAKING**: hacer obligatorio `client` en `POST /auth/login`. Su ausencia, valor nulo o valor vacio/en blanco se rechaza con `400 Bad Request`; un valor aceptado debe aparecer como claim `client` en el token.
- Mantener la respuesta exitosa con `200 OK` y los campos `token`, `expiresIn`, `user` y `debug`. `expiresIn` conserva el valor documentado de 3600 segundos y debe coincidir con `exp - now` dentro de una tolerancia documentada.
- Rechazar por verificacion los tokens expirados y cubrir ese comportamiento con pruebas, sin introducir un endpoint nuevo.
- Evitar que tokens, secretos u otros valores sensibles aparezcan en logs.
- Documentar la decision entre HMAC y una libreria JWT en una etapa posterior; esta propuesta no elige una tecnologia ni define el diseno de implementacion.

## Capabilities

### New Capabilities
- `auth-token`: emision de tokens de autenticacion firmados con claims minimos y expiracion verificable.

### Modified Capabilities
- Ninguna. El repositorio no tiene specs existentes; el contrato documentado de `POST /auth/login` se toma como baseline y sus cambios intencionales se especifican en la nueva capability.

## Impact

La superficie afectada es `POST /auth/login`, el significado del string `token`, la validacion del campo de entrada `client`, la configuracion del secreto y el comportamiento de registro de datos sensibles. Se preservan los campos de respuesta y las respuestas de autenticacion del contrato Day 1 salvo el nuevo rechazo `400` para `client` ausente o en blanco. La issue no define una allowlist ni otros criterios de validez para `client`; no se propone una en esta etapa.

El baseline es [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md). La semantica del token y la opcionalidad de `client` cambian expresamente por los requisitos de la Issue #12; el resto del contrato de login se conserva.