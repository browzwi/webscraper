# Repository Guidelines

## Project Structure & Key Modules
- `src/main/java/com/browzwi/webscraper` – Spring MVC entry point (`WebScraperApplication`) plus layered packages: `web` (controllers + Thymeleaf adapters), `web/dto` (forms), `service` (business logic, Quartz schedulers, scraper engine), `service/job` (Quartz jobs), `service/settings` (app settings), `scraper/model` & `scraper/service` (YAML recipe objects, HtmlUnit/Playwright fetchers, HTML processing, Markdown conversion), `storage` (file persistence), `domain` (JPA entities), `repository` (Spring Data JPA interfaces), `security` (custom user details + dev admin bootstrapping).
- `src/main/resources/templates` – Layout + pages (`dashboard`, `recipes`, `jobs`, `settings`, `error`). All views extend `layout.html` via `~{layout :: layout(~{::main})}` to inherit Tailwind/HTMX scripts, CSRF logic, and nav.
- `src/main/resources/db` – Liquibase changelog (`db.changelog-1.0.yaml`) managing UUID-as-CHAR schema, scraper tables, job tables, and `app_settings`.
- Profiles: `application.yml` (shared defaults), `application-dev.yml` (MySQL root/secret, dev admin password, Playwright knobs). Tests use `src/test/resources/application.yml` (H2 + Liquibase).
- Tests mirror the main packages under `src/test/java` with Mockito and stub fetchers (`ScraperEngineTest`, `ScrapeJobServiceTest`, etc.).

## Design Patterns & Architecture Notes
- **Layered MVC** – Controllers only orchestrate view models and delegate to services; services coordinate repositories, storage, and external fetchers; repositories remain thin Spring Data interfaces.
- **DTO/Form Pattern** – Every Thymeleaf form (recipes, jobs, settings) binds to a dedicated DTO (`RecipeForm`, `JobForm`, `SettingsForm`) to keep controllers decoupled from entities.
- **Strategy for Fetching** – `ScraperEngine` selects between `HtmlFetcher` and `PlaywrightFetcher` at runtime based on `SettingsService` (set in `/settings`). Fetchers implement a shared `PageFetcher` interface.
- **Template Composition** – All views compose via `layout.html` and fragments (`recipes/test-result`, `jobs/target-details`, `error.html`). HTMX handles partial swaps (e.g., recipe tests, job target drill-down) with CSRF headers injected from the layout script.
- **Quartz Scheduling** – `ScrapeJobSchedulerService` registers jobs with Quartz using `ScrapeQuartzJob`. Job metadata (`ScrapeJob`, `ScrapeTarget`, `ScrapeResultData`) persists in MySQL, while raw/processed HTML goes to the filesystem via `FileStorageService`.
- **Resilience & Observability** – Global `error.html` + specific `error/403.html` ensure user-friendly failures; controllers surface validation errors with Tailwind-styled alerts. Dev-only security initialiser updates passwords via repository-level update to avoid optimistic-lock issues.

## Build, Run & Test Commands
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` – launches the full stack (MySQL, Thymeleaf UI, Quartz, HtmlUnit/Playwright options) with tailwind/HTMX.
- `./mvnw test` – runs all JUnit tests on H2 (includes Liquibase migrations, scraper engine tests, scheduler/service tests).
- `./mvnw clean verify` – full build + tests + Liquibase validation.
- `./mvnw -Pproduction package` – builds a deployable JAR without dev tooling.

## Coding & UI Guidelines
- Java 17, four-space indents, constructor injection only. Keep package boundaries clean (no repository access from controllers). Use `@Transactional` only inside services.
- Thymeleaf: always reference fragments via `~{...}`; keep forms inside `<form th:object="...">` blocks with hidden `_csrf` input unless the layout injects HTMX headers.
- HTMX actions should target partial fragments (`:: result`, `:: content`), provide copy/beautify features, and use `hx-vals="js:{...}` when sending raw HTML strings.
- Liquibase: append incremental changesets to `db.changelog-1.0.yaml`; keep IDs ordered; default UUID columns to `CHAR(36)`.
- Storage: use `FileStorageService.resolveJobDir/resolveTargetDir` to keep job artifacts predictable under `webscraper.storage.root` (default `./data`).

## Testing Guidelines
- Prefer Mockito to isolate repositories/services (`@ExtendWith(MockitoExtension.class)`).
- For HTML parsing/extraction, use static HTML strings and stub fetchers like in `ScraperEngineTest` to assert structured data, processed HTML, and Markdown outputs.
- For Quartz/job flows, stub repositories to avoid hitting real DBs; assert scheduler invocations in `ScrapeJobServiceTest`.
- Always run `./mvnw test` before pushing; ensure new Liquibase changes run successfully on both H2 and MySQL.

## Configuration & Operational Notes
- Activate the correct profile (`dev`, `test`, etc.) via `-Dspring-boot.run.profiles=...`. The dev profile assumes MySQL is running with `root/secret` credentials and Liquibase has applied the schema.
- `/settings` persists the preferred scraper engine (HtmlUnit vs Playwright) in `app_settings`. Playwright fetches require the browser binaries – configure via `webscraper.fetcher.playwright.*` (browser/headless/timeout) if needed.
- Admin credentials in dev/test are controlled by `webscraper.security.admin.*`; passwords reset automatically at startup without triggering optimistic-lock failures.
- Error handling routes through Thymeleaf templates (`error.html`, `error/403.html`) so users never see the default Whitelabel page.
- Never commit real secrets; rely on profile overrides, env vars, or secret managers in production.
