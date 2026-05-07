# Repository Guidelines

## Project Structure & Module Organization
- Java source lives in `src/main/java/com/techproof`, organized by responsibility.
- `checker/` handles grammar and typo checks; `docx/` parses Word files; `dictionary/` loads typo data; `model/` stores domain objects; `ui/` contains JavaFX screens.
- Entry point: `com.techproof.TechProofApp`.
- Runtime resources are in `src/main/resources`, including `dictionary/typo-dictionary.json`.
- Build configuration is in `build.gradle`; wrapper scripts are `gradlew` and `gradlew.bat`.

## Build, Test, and Development Commands
- `./gradlew.bat run` (Windows) or `./gradlew run` (Unix): run the JavaFX app locally.
- `./gradlew.bat clean build`: compile, test, and package the JAR.
- `./gradlew.bat test`: run test tasks only.
- `./gradlew.bat clean`: remove generated build outputs.

## Coding Style & Naming Conventions
- Target Java 21 (Gradle toolchain is already configured).
- Use 4-space indentation, UTF-8 encoding, and readable line lengths (prefer under ~120 chars).
- Naming: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Keep package names lowercase and feature-oriented (example: `com.techproof.checker`).
- Keep UI logic in `ui/` and checking logic in `checker/`; avoid mixing concerns.

## Testing Guidelines
- Add tests under `src/test/java`, mirroring production package structure.
- Use JUnit 5 for unit tests.
- Test class naming: `<ClassName>Test` (example: `ParticleCheckerTest`).
- Focus coverage on parsing rules, particle checks, and dictionary edge cases.
- Run `./gradlew.bat test` before opening a PR.

## Commit & Pull Request Guidelines
- No Git history is available in this workspace; use Conventional Commit style by default.
- Examples: `feat: add quoted-text exception to particle checker`, `fix: handle empty paragraph in DocxReader`.
- PRs should include purpose, key code changes, verification commands, and screenshots for UI updates.
- Link related issues and keep each PR scoped to one logical change.

## Security & Configuration Tips
- Do not commit documents containing private or sensitive content.
- Treat dictionary edits in `src/main/resources/dictionary/typo-dictionary.json` as code changes and review carefully.
