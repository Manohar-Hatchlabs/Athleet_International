# Athleet International

Platform for Athleet's two programs — **AJSDP** (school-based junior sports development) and **UKSC Academy** (football academy) — with a Web admin, Parent app and Staff app.

## Repository layout

```
backend/        Spring Boot (Java 21, Maven) modular monolith — common API for web and apps
  athleet-core/   shared: auth, users/roles/scopes, notifications, storage, audit, consent, support
  athleet-ajsdp/  AJSDP-only modules
  athleet-uksc/   UKSC-only modules
  athleet-app/    Spring Boot entry point and configuration
docs/           project documentation
.claude/skills/athleet-backend/SKILL.md   backend architecture and conventions
```

Web admin (React) and mobile apps (React Native) will be added later.

Requirements: [Athleet Platform – Functionality Document](https://claude.ai/artifact/DQLuehzhzvhBoUcZkJsb3g)
