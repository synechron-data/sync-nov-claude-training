---
name: auth-endpoint-enumeration-and-abuse
description: /api/auth login+register flaws - register echoes "Email already registered", login timing oracle, no rate limit, no @Valid, case-sensitive email, token sent to all hosts
metadata:
  type: feedback
---

Auth module audit (2026-11-18) found these on top of [[jwt-validation-fail-open]]:
- `AuthService.register` throws "Email already registered: <email>" and it is returned as a 400, so anyone can check which emails exist. `login` returns the same message either way, but it skips BCrypt when the email is unknown, so response time reveals whether an account exists.
- No rate limiting or lockout on `/api/auth/**`. `@Valid` is missing, so empty/null/oversized passwords are accepted. A null email or displayName causes a 500.
- Emails are not lower-cased or trimmed, so `A@x.com` and `a@x.com` become two separate accounts. There is no email verification.
- Mass assignment is NOT an issue: the request records have explicit fields and `User` has no role field.
- Frontend `auth.interceptor.ts` attaches the Bearer token to every request, whatever the host. `/tasks` has no route guard. `logout()` and `isLoggedIn()` are never called.
- CSRF disabled is acceptable because the token goes in a header, not a cookie. CORS is limited to localhost:4200 with no credentials.

**Why:** These are the recurring auth-endpoint findings. On a re-audit, check whether each one has been fixed rather than rediscovering them.
**How to apply:** Re-verify the AuthService.register/login code paths and SecurityConfig. Related: [[config-files-access-denied]], [[dependency-versions-baseline]].
