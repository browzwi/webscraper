# Repository Guidelines

## Project Structure & Module Organization
Java sources live under <code>src/main/java/com/browzwi/officefinder</code>, anchored by OfficefinderApplication plus Vaadin UI, REST, and Quartz layers. Configuration, templates, and static assets belong in <code>src/main/resources</code>; prefer profile overrides such as <code>application-local.properties</code> over editing defaults. Tests are in <code>src/test/java</code>, with TestcontainersConfiguration wiring MongoDB, MySQL, and Ollama so integration suites stay hermetic.

## Build, Test, and Development Commands
- <code>./mvnw spring-boot:run</code> launches the dev server on port 8080 with devtools reloads and default containers.
- <code>./mvnw clean verify</code> performs a full clean build, runs unit and integration tests, and validates the Vaadin frontend pipeline.
- <code>./mvnw -Pproduction package</code> compiles an optimized executable JAR and runs the Vaadin frontend goals required for deployment.
- <code>./mvnw test -Dtest=TestOfficefinderApplication</code> is useful for iterating on a focused integration test class.

## Coding Style & Naming Conventions
Target Java 17 with four-space indentation, no tabs, and trailing newlines in every file. Keep packages singular and layered (examples: <code>com.browzwi.officefinder.scraper</code>, <code>.scheduler</code>, <code>.ui</code>). Classes and records use PascalCase; methods, fields, and variables use camelCase. Annotate Spring beans (<code>@Service</code>, <code>@Repository</code>, <code>@RestController</code>) and prefer constructor injection plus immutable DTO records named <code>*Request</code>/<code>*Response</code>. Property keys remain lowercase dot notation, and Liquibase changelog files follow <code>V&lt;timestamp&gt;__description.xml</code>.

## Testing Guidelines
JUnit Jupiter, Spring Boot test starters, security utilities, and Testcontainers are already available. Name suites <code>*Tests</code> and focused specs <code>Test*</code>. Unit tests should mock external APIs; integrations can extend TestOfficefinderApplication to inherit the container lifecycle. Run <code>./mvnw test</code> before committing and <code>./mvnw clean verify</code> before opening a PR, ensuring both success and failure paths for schedulers, repositories, and AI prompts are exercised.

## Commit & Pull Request Guidelines
Commits follow Conventional Commit prefixes (feat, fix, chore) with imperative summaries under ~70 characters. PRs must include: intent summary, linked issue or ticket, screenshots or GIFs for Vaadin UI changes, Liquibase/database notes if schema changes occur, and the exact test command plus result. Mention new environment variables or secrets so reviewers can reproduce the setup.

## Security & Configuration Tips
Never hard-code credentials or AI provider keys; source them from environment variables or Spring Cloud Config. Disable local Testcontainers when pointing at shared Mongo/MySQL instances via <code>spring.profiles.active</code>. Rotate Mistral/Ollama tokens regularly and double-check scheduler cron expressions before promoting production profiles.
