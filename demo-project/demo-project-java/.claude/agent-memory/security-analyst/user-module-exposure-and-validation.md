---
name: user-module-exposure-and-validation
description: State of the backend user module (User entity, UserRepository) and its only consumer AuthService as of 2026-11-18 audit - no user endpoints, latent hash leak, weak registration validation
metadata:
  type: feedback
---

Audit 2026-11-18 of backend/.../user/ (only User.java + UserRepository.java exist; no controller/service/DTO):
- No endpoint returns `User`; only consumer is `auth/AuthService` (register/login return just a JWT). No Spring Data REST / actuator in pom, so the repo is not auto-exposed.
- Latent leak: `User.passwordHash` has a public getter and no `@JsonIgnore`, so it would serialize if any future endpoint returns the entity.
- No roles/authorities anywhere (filter grants emptyList); no mass assignment (RegisterRequest record has only email/password/displayName).
- Repository uses derived queries only, so no injection risk.
- Registration: `@Valid` missing, so an empty password is accepted, email is unvalidated and not normalized (case-sensitive uniqueness), and over-long fields cause 500s. Check-then-save race on existsByEmail.
- Enumeration and rate limiting are tracked in [[auth-endpoint-enumeration-and-abuse]]. Do not duplicate them here.
- H2 console permitAll would expose app_user hashes if it is enabled (unverified, see [[config-files-access-denied]]).

**Why:** Re-audits should verify fixes rather than rediscover these issues.
**How to apply:** On the next user/auth audit, first check for a new UserController or DTO returning `User`, `@JsonIgnore` on passwordHash, `@Valid` on AuthController. Related: [[jwt-validation-fail-open]], [[task-module-idor-and-validation]].
