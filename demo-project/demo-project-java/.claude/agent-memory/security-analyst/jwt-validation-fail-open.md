---
name: jwt-validation-fail-open
description: JwtUtil.isTokenValid always returns true (deliberate training bug); expiry/signature only enforced as a side effect of extractUserId parsing
metadata:
  type: feedback
---

`backend/.../auth/JwtUtil.isTokenValid` returns `true` unconditionally (documented as a deliberate Module 13 training bug — report it, don't treat it as unknown). jjwt 0.12.5 `parseSignedClaims` in `extractUserId` still rejects bad signature / expired / unsigned tokens, but the exception escapes `JwtAuthFilter` uncaught (-> empty 403 via /error), so protection is accidental and fail-open by design. Also: `secret.getBytes()` uses platform charset; email PII in JWT claims; frontend stores JWT in localStorage.

**Why:** Severity hinges on the side-effect parse; rate it Medium-High, not Critical, unless extractUserId changes.
**How to apply:** Re-check both methods on each audit; if extractUserId ever stops verifying, escalate to Critical. Related: [[task-module-idor-and-validation]].
