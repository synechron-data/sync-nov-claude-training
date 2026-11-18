---
name: config-files-access-denied
description: backend/src/main/resources (application.properties) is blocked by permission settings; JWT secret and H2 console config cannot be verified
metadata:
  type: feedback
---

Reading `backend/src/main/resources/application.properties` fails with "directory denied by your permission settings" (2026-11-18). Glob also returns nothing for that folder.

**Why:** JWT secret strength/hardcoding and `spring.h2.console.enabled` cannot be confirmed; SecurityConfig permits `/h2-console/**` and disables frame-options globally.
**How to apply:** State these as unverified/conditional findings and ask the caller to confirm values manually. Don't retry the read unless permissions changed. Related: [[jwt-validation-fail-open]].
