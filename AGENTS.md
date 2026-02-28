```markdown
# AGENTS.md Guidelines

## 1. Purpose:

This document outlines guidelines for the development of AI coding agents within the AGENTS.md repository. These guidelines are designed to ensure code quality, maintainability, and adherence to best practices.  The primary goal is to create a robust and scalable system that prioritizes productivity and reduces technical debt.

## 2. Principles:

* **DRY (Don't Repeat Yourself):**  Avoid duplicating code.  Refactor any duplicated logic into reusable components.
* **KISS (Keep It Simple, Stupid):**  Strive for the simplest solution that meets the requirements. Avoid over-engineering.
* **SOLID Principles:**  The code shall adhere to SOLID principles to ensure object-oriented design and maintainability.  Specifically, the code should be easy to understand, and modular, and follow principles of single responsibility.
* **YAGNI (You Aren't Gonna Need It):**  Only implement features that are currently required; don't add functionality prematurely.
* **Productivity Focus:**  All development must prioritize delivering value and moving toward completed features.  No time wasted on unnecessary code.

## 3. File Structure & Scope:

* **File Size Limit:** Each file must be no more than 180 lines of code.
* **Categorization:**  Files should be organized into logical categories reflecting the core functionality of the agent.  Examples: `core_logic`, `data_processing`, `communication`, `monitoring`.
* **Module Decomposition:**  Break down large modules into smaller, focused, and testable units.
* **Naming Conventions:**  Use consistent and descriptive naming conventions.

## 4. Code Style & Formatting:

* **Indentation:** Use 2 spaces for indentation.
* **Line Length:** Maximum 120 characters per line.
* **Whitespace:** Use whitespace strategically for readability.
* **Comments:**  Add concise, informative comments where necessary, but avoid excessive commenting.
* **Formatting:** Follow a consistent code formatting style (e.g., using a code formatter).

## 5. Testing:

* **Mocking Only:** All tests *must* utilize mocks and stubs.  Do NOT use real implementations.
* **Test Coverage Target:**  Achieve a minimum of 80% test coverage.
* **Test Structure:** Tests should be structured logically, covering all critical functionalities and edge cases.
* **Test Command:** Use a standard testing command (e.g., `pytest` or a custom script) to run tests.  Ensure tests are easily runnable.
* **Test Data:**  Use realistic, albeit simplified, test data.

## 6. Specific Rules & Considerations:

* **Agent Class:** The core agent class (defined in the `agents.py` file) must be a self-contained, well-documented unit.
* **Data Handling:**  All data manipulation routines should be designed with immutability in mind. Use appropriate data structures.
* **Communication Protocol:** The communication protocol for data exchange between agents must be clearly defined and documented.
* **Error Handling:** Implement robust error handling to gracefully manage unexpected situations.
* **Logging:** Introduce meaningful logging to aid debugging and monitoring.
* **Configuration:**  Implement a flexible configuration system to allow for easy adaptation to different environments.
* **Version Control:**  Use Git for version control and adhere to established Git practices.
* **Documentation:**  Document all functions, classes, and modules with clear and concise documentation using tools like Sphinx or a similar system.

## 7.  Production-Ready Considerations:

* **Readability:** Prioritize code readability and maintainability above all else.
* **Scalability:** Design the code to be easily scalable to handle increasing data volumes or user load.
* **Modularity:**  Ensure that the code is modular and can be easily integrated with other systems or components.
* **Error Prevention:**  Implement checks and validations to prevent errors from propagating through the code.
* **Security:** Consider security implications when designing the system (e.g., input validation, authentication).

## 8.  Compliance:

* This document reflects the current understanding of AGENTS.md and is subject to change.  Any modifications must be formally documented and reviewed.

## 9. Future Considerations:

* Consider adding unit tests for the core agent algorithms.
* Explore automated documentation generation (e.g., using Sphinx).
* Implement more sophisticated monitoring and alerting mechanisms.

```