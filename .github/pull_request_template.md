## 🚀 Pull Request Checklist (focused for use on `main` branch)


### 🔍 General
- [ ] The purpose of this PR is clearly described
- [ ] The PR merges cleanly with no unresolved conflicts
- [ ] All review comments and change requests from previous reviewers have been addressed

### 🧪 Code Quality
- [ ] Code compiles and runs locally without errors
- [ ] Unit tests are written for new or modified logic
- [ ] Existing unit/integration tests pass (`./gradlew clean build`)
- [ ] No commented-out or debug code (e.g., `System.out.println`)
- [ ] Code follows the project’s style and formatting rules

### 🛡️ Main Branch Rules
- [ ] **No `-SNAPSHOT` versions present in `build.gradle.kts` or any dependency**
- [ ] No temporary or test code (e.g., dummy endpoints, sample services, etc.)
- [ ] Versioning has been bumped appropriately (if applicable) — no development versions left

### 📝 Documentation
- [ ] Public APIs or endpoints are documented (Swagger/OpenAPI, Markdown, or Javadoc)
- [ ] README or related documentation has been updated (if necessary)

### 🔁 Review & Approval
- [ ] At least one reviewer has approved the changes
- [ ] CI pipeline has passed successfully

---

Please ensure the PR description below explains the **what** and **why**:

Summary
<!-- A short summary of the changes introduced -->
Motivation
<!-- Why are these changes needed? What problem is solved? -->
Notes
<!-- Any additional context or known issues -->
