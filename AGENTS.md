# AGENTS.md

App Android de detecção de presença e anti-furto (`app/`), com backend de sensor Wi-Fi (`backend/`), firmware ESP32
(`firmware/`) e esqueleto Flutter (`mobile/`). Detalhes em `README.md`; convenções Android em `gemini.md`.

## Stack
- `app/`: Kotlin 2.4.21, AGP 9.3.1, JDK 21, minSdk 33, ViewBinding, coroutines, OkHttp.
- `backend/`: Node 20, TypeScript, `better-sqlite3`, Jest. Camadas `domain/`, `infrastructure/`, `presentation/`.
- `mobile/`: Flutter, Dart `^3.11.0`, só `lib/domain` por enquanto.

## Comandos
- App (na raiz): `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew spotlessCheck` (`spotlessApply` corrige),
  `./gradlew jacocoTestReport jacocoTestCoverageVerification` (mínimo 80%). Precisa de `local.properties` com `sdk.dir`.
- Backend (`cd backend`): `npm ci`, `npm run lint`, `npm run test:cov`, `npm run build`.
- Mobile (`cd mobile`): `flutter pub get`, `dart format --output=none --set-exit-if-changed .`, `flutter analyze`,
  `flutter test --coverage`.

## Gates de CI
- `main.yml`: Flutter (format, analyze, test, cobertura de `lib/domain` = 100%, build APK) e backend (lint, `test:cov`
  com limiar global 100%, build). Não roda o Gradle do app Android: rode os comandos do app localmente.
- `validate.yml` e `security.yml`: reusables de `juninmd/base-actions` (validação do PR e Gitleaks).
- `release-drafter.yml`: rascunho de release.

## Não fazer
- Não commitar segredos: `.env`, keystores, `google-services.json`, token do Telegram, `API_KEY`. Só nomes de variáveis em docs.
- Não commitar `backend/node_modules/`, `coverage/` ou logs de build (`.gitignore` já cobre; há arquivos antigos rastreados).
- Não baixar o limiar de cobertura nem desligar regra de lint para passar o gate.
- Não editar `package-lock.json` nem `pubspec.lock` à mão.
- Código, identificadores e commits em inglês; documentação em pt-BR.

<!-- base-actions-main-policy -->
## GitHub Actions — padrão obrigatório (@main)

- Referencie as actions internas sempre pelo branch, nunca por SHA:
  - `uses: juninmd/base-actions/<action>@main`
  - `uses: juninmd/base-actions/.github/workflows/<arquivo>.yml@main`
- Não fixe `@<sha>` em `juninmd/base-actions`: a atualização é centralizada no
  `main` e passa a valer no próximo run.
- PRs cujos checks não iniciam por billing/spending limit são mesclados pelo bot central
  `juninmd/base-actions/.github/workflows/auto-merge-billing-blocked.yml`.
  Para impedir o merge automático de um PR, aplique a label `do-not-merge`.