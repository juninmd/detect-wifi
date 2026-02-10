# Security Policy

## Supported Versions

The following versions of the Presence Detector project are currently being supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.1.x   | :white_check_mark: |
| 1.0.x   | :x:                |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of our software seriously. If you believe you have found a security vulnerability in the Presence Detector project, please report it to us as described below.

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to `security@example.com`. You should receive a response within 48 hours. If possible, encrypt your message with our PGP key (if available).

## Security Best Practices for Contributors

*   **Never commit secrets**: Ensure API keys, passwords, and tokens are not committed to the repository. Use environment variables or `local.properties`.
*   **Validate Inputs**: All user inputs must be validated to prevent injection attacks.
*   **Keep Dependencies Updated**: Regularly update dependencies to patch known vulnerabilities.
*   **Review Code**: All code changes must undergo a security review before merging.
