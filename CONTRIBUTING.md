# Contributing to Presence Detector

We welcome contributions to Presence Detector! Please follow these guidelines to ensure a smooth collaboration process.

## Getting Started

1.  **Fork the repository** and clone it locally.
2.  **Install dependencies** by running the project in Android Studio or using `./gradlew build`.
3.  Ensure you have **JDK 21** installed as it is required for the build.

## Development Workflow

1.  Create a new branch for your feature or bug fix:
    ```bash
    git checkout -b feature/my-feature
    ```
2.  Make your changes.
3.  **Run tests** to ensure no regressions:
    ```bash
    ./gradlew testDebugUnitTest
    ```
4.  **Format your code** using Spotless (Google Style):
    ```bash
    ./gradlew spotlessApply
    ```
    *Note: The CI pipeline will fail if code is not formatted correctly.*

## Code Quality

We enforce strict code quality standards:

*   **Linting:** Android Lint checks must pass (`./gradlew lintDebug`).
*   **Formatting:** Code must be formatted using `ktfmt` (Google Style) via Spotless.
*   **Coverage:** Code coverage must remain above 80% (`./gradlew jacocoTestCoverageVerification`).
*   **Security:** No secrets in code. Gitleaks scan runs on CI.

## Pull Requests

1.  Push your branch to your fork.
2.  Open a Pull Request against the `main` branch.
3.  Fill out the PR template with details about your changes.
4.  Wait for CI checks to pass.
5.  Address any review comments.

## CI/CD Pipeline

Our CI pipeline (GitHub Actions) is defined in `.github/workflows/ci.yml` and runs the following stages:

### 1. Lint
*   **Spotless:** Checks Kotlin code formatting.
*   **Android Lint:** Checks for Android-specific issues.

### 2. Test
*   **Unit Tests:** Runs all unit tests.
*   **Coverage:** Generates JaCoCo reports and verifies 80% coverage.
*   **Artifacts:** Uploads test results and coverage reports.

### 3. Security
*   **Gitleaks:** Scans for exposed secrets/keys.
*   **Trivy:** Scans dependencies for known vulnerabilities.

### 4. Build
*   Builds both Debug and Release APKs.
*   Uploads APKs as build artifacts.

### 5. Deploy (Main Branch Only)
*   Triggered automatically when code is pushed to `main`.
*   Creates a new GitHub Release with the version number based on the run number (e.g., `v1.1.42`).
*   Attaches the generated APKs to the release.

## Environment Variables
The build system uses the following environment variables (configured via GitHub Secrets for CI):
*   `VERSION_CODE`: Integer version code.
*   `VERSION_NAME`: String version name.
*   `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`: For signing release builds.
