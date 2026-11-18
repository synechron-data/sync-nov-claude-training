---
name: dependency-versions-baseline
description: Resolved backend dependency versions from mvn dependency:tree on 2026-11-18 (Spring Boot 3.2.5 line, out of OSS support)
metadata:
  type: feedback
---

2026-11-18 `mvn -f backend/pom.xml dependency:tree`: Spring Boot 3.2.5, spring-core/web/webmvc 6.1.6, spring-security 6.2.4, tomcat-embed-core 10.1.20, jackson-databind 2.15.4, logback 1.4.14, snakeyaml 2.2, h2 2.2.224, jjwt 0.12.5.

**Why:** Boot 3.2.x is past OSS end-of-life; several Spring/Tomcat CVEs published after these versions (notably Spring Security BCrypt >72-byte password issue CVE-2025-22228, which affects this app's BCryptPasswordEncoder).
**How to apply:** Compare against this baseline next time; if the pom was upgraded, update this note.
