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

*   **Linting:** Android Lint checks must pass.
*   **Formatting:** Code must be formatted using `ktfmt` (Google Style).
*   **Coverage:** Code coverage must remain above 80%.
*   **Security:** No secrets in code. Gitleaks scan runs on CI.

## Pull Requests

1.  Push your branch to your fork.
2.  Open a Pull Request against the `main` branch.
3.  Fill out the PR template with details about your changes.
4.  Wait for CI checks to pass.
5.  Address any review comments.

## CI/CD Pipeline

Our CI pipeline (GitHub Actions) runs the following checks:

*   **Lint:** Android Lint and Spotless check.
*   **Test:** Unit tests, Integration tests (Robolectric), and JaCoCo coverage verification (min 80%).
*   **Build:** Assembles Debug and Release APKs.
*   **Security:** Scans for secrets (Gitleaks) and dependency vulnerabilities (Trivy).

## Environment Variables

The CI pipeline uses the following secrets for signing releases and reporting:

*   `RELEASE_KEYSTORE_BASE64`: Base64 encoded keystore file.
*   `KEYSTORE_PASSWORD`: Password for the keystore.
*   `KEY_ALIAS`: Key alias.
*   `KEY_PASSWORD`: Key password.
*   `CODECOV_TOKEN`: Token for uploading coverage reports to Codecov.

Upon merging to `main`, a new release is automatically created and deployed.
