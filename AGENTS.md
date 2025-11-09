# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/browzwi/webscraper` – Spring MVC entry point (`WebScraperApplication`) plus layered packages: `web` (controllers + Thymeleaf adapters), `web/dto` (forms), `service` (business logic, Quartz schedulers, scraper engine), `service/job` (Quartz jobs), `service/settings` (app settings), `scraper/model` & `scraper/service` (YAML recipe objects, HtmlUnit/Playwright fetchers, HTML processing, Markdown conversion), `storage` (file persistence), `domain` (JPA entities), `repository` (Spring Data JPA interfaces), `security` (custom user details + dev admin bootstrapping).
- `src/main/resources/templates` – Layout + pages (`dashboard`, `recipes`, `jobs`, `settings`, `error`). All views extend `layout.html` via `~{layout :: layout(~{::main})}` to inherit Tailwind/HTMX scripts, CSRF logic, and nav.
- `src/main/resources/db` – Liquibase changelog (`db.changelog-1.0.yaml`) managing UUID-as-CHAR schema, scraper tables, job tables, and `app_settings`.
- Profiles: `application.yml` (shared defaults), `application-dev.yml` (MySQL root/secret, dev admin password, Playwright knobs). Tests use `src/test/resources/application.yml` (H2 + Liquibase).
- Tests mirror the main packages under `src/test/java` with Mockito and stub fetchers (`ScraperEngineTest`, `ScrapeJobServiceTest`, etc.).

## Build, Test, and Development Commands
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` – launches the full stack (MySQL, Thymeleaf UI, Quartz, HtmlUnit/Playwright options) with tailwind/HTMX.
- `./mvnw test` – runs all JUnit tests on H2 (includes Liquibase migrations, scraper engine tests, scheduler/service tests).
- `./mvnw clean verify` – full build + tests + Liquibase validation.
- `./mvnw -Pproduction package` – builds a deployable JAR without dev tooling.

## Coding Style & Naming Conventions
- Target Java 17 with 4-space indentation and same-line braces; adhere to standard Spring style.
- Keep packages lowercase; classes use PascalCase, methods and fields camelCase, constants UPPER_SNAKE_CASE.
- Favor constructor injection, explicit ` @RequestMapping` paths, and dedicated packages for DTOs, services, and repositories.
- Run your IDE formatter before committing; introduce a Maven formatter plugin if the team standardizes on one.

## Priority Design Notes

### ⚠️ Critical: Modal Form Object Initialization

**Always ensure all form objects referenced in Thymeleaf templates are properly initialized in controller methods.** Missing form objects cause `IllegalStateException` errors when templates try to access them.

**Prevention Checklist:**
1. Check all `th:object="${formName}"` references in templates
2. Ensure corresponding form objects are added to the model in ALL controller methods that serve that page
3. Use a shared `preparePage` method across controllers to ensure consistency
4. Add required UI data objects (roleOptions, memberName, memberEmail, etc.) to the model
5. Verify all controllers that prepare the same page model include the same form objects

**Example of proper model preparation:**
```java
private void prepareTeamPage(Model model) {
    // Always initialize all required form objects
    model.addAttribute("teamEditForm", new TeamEditForm(null, null, null, null));
    model.addAttribute("teamRemoveForm", new TeamRemoveForm(null, null, null, ""));
    model.addAttribute("teamInviteForm", new TeamInviteForm(null, null, DesiredAccountRole.MEMBER, null, new ArrayList<>(), InviteExpiry.SEVEN_DAYS, true));
    model.addAttribute("roleForm", new RoleForm(null, null, null, new ArrayList<>()));
    
    // Always add required UI data
    model.addAttribute("roleOptions", teamService.getRoleOptions());
    model.addAttribute("memberName", null);
    model.addAttribute("memberEmail", null);
    
    // Other model attributes...
}
```

## Design Principles
- **Single Responsibility Principle (SRP)**: A class should have only one reason to change. This means that a class should be responsible for only one thing.
- **Open/Closed Principle (OCP)**: A class should be open for extension but closed for modification.
- **Liskov Substitution Principle (LSP)**: Objects of a superclass should be replaceable with objects of a subclass without affecting correctness.
- **Interface Segregation Principle (ISP)**: Clients should not be forced to depend on methods they do not use.
- **Dependency Inversion Principle (DIP)**: High-level modules should not depend on low-level modules; both should depend on abstractions.
- **Don't Repeat Yourself (DRY)**: Avoid duplicating code or logic to make maintenance easier.
- **Keep It Simple, Stupid (KISS)**: Keep your code as simple as possible to avoid unnecessary complexity.
- **You Ain't Gonna Need It (YAGNI)**: Do not add functionality until it is actually needed.
- **Single Level of Abstraction (SLA)**: Maintain a single level of abstraction in your code to improve readability.
- **Law of Demeter (LoD)**: A module should not know about the internal workings of the objects it uses.

## Code Documentation Standards

### Javadoc Requirements
- All classes, interfaces, and public methods must include purpose-driven Javadoc comments following these principles:
- Remember: The best comments explain the business problem being solved and the design decisions made to solve it. If your comment could apply to any similar method, it's too generic.

**Class-Level Documentation Structure:**
```java
/**
 * Business purpose statement explaining domain role.
 *
 * <p>Architectural rationale: Design pattern choice and collaboration context.
 *
 * <p>Key constraints: Security, performance, or business rules affecting design.
 *
 * @since version
 */
```

**Method-Level Documentation Structure:**
```java
/**
 * Business operation purpose and domain context.
 *
 * <p>Implementation rationale: Why specific approaches were chosen,
 * error handling strategy, and non-obvious design decisions.
 *
 * @param paramName business meaning and validation constraints
 * @return business entity state and expectations
 * @throws ExceptionType business scenario triggering this exception
 */
```

### Documentation Focus Rules
- **Explain "WHY" not "WHAT"** - Code shows implementation, comments explain business rationale
- **Business Context First** - Document domain requirements and constraints driving design decisions
- **Decision Rationale** - Explain non-obvious technical choices and their business impact
- **Error Scenarios** - Describe business conditions causing exceptions, not technical mechanics
- **Usage Guidance** - Provide context for when and how other developers should use the code
- **Avoid Implementation Details** - Focus on contract, behavior, and business rules

### Comment Quality Standards
- Use concrete, specific language over vague terms like "handles," "manages," or "processes"
- Reference domain concepts and business constraints directly
- Explain architectural decisions that support business requirements
- Document security, performance, or compliance considerations when relevant

## Coding & UI Guidelines
- Java 17, four-space indents, constructor injection only. Keep package boundaries clean (no repository access from controllers). Use `@Transactional` only inside services.
- Thymeleaf: always reference fragments via `~{...}`; keep forms inside `<form th:object="...">` blocks with hidden `_csrf` input unless the layout injects HTMX headers.
- HTMX actions should target partial fragments (`:: result`, `:: content`), provide copy/beautify features, and use `hx-vals="js:{...}` when sending raw HTML strings.
- Liquibase: append incremental changesets to `db.changelog-1.0.yaml`; keep IDs ordered; default UUID columns to `CHAR(36)`.
- Storage: use `FileStorageService.resolveJobDir/resolveTargetDir` to keep job artifacts predictable under `webscraper.storage.root` (default `./data`).

- **Thymeleaf Typesafety**
    - Prioritize typesafe objects in the controller.
    - Templates should only handle pre-processed, safe objects.
    - Avoid complex and nested logic within Thymeleaf expressions.
    - All data passed to templates should be ready for direct rendering, minimizing complex SpEL expressions.

- **Documentation & Code Quality**
    - Always document the code you make using Javadoc and Clean Code methodology
    - When dealing with coding, always prioritize the much simple solution, less side effects and less moving parts

- **Exception Handling**
    - When coding do not repurpose existing classes for something that is it not intended for
        - Example: using `UsernameNotFoundException` exception when the actual error is `UsernameNoRoleException` or `UsernameMissingRoleException`
    - Use typed exceptions with error codes for validation failures to enable proper UI error handling
        - Create domain-specific error code enums that bind machine-readable codes to human-readable messages
        - Example: `BookingValidationErrorCode` enum with codes like "no_availability_for_date"
        - Exception classes should include both the error code and message to support in-modal error displays vs. system-wide toast notifications
        - This allows UI components to distinguish between validation errors (show in modal) and system errors (show as toast)

- **Validation & Error Prevention**
    - Make class methods to have preconditions like using Spring Assertions and Bean Validation
    - Always look for potential Thymeleaf NPEs - always add null checks on the template

- **Frontend Guidelines**
    - Use Javascript or Hotwire Stimulus only for UI interactions
    - Any business logic or complex logic should be done through Thymeleaf expression language
    - **Important**: Prioritize the use of **Tailwind Utility CSS**. Avoid writing vanilla CSS. Only use custom CSS when a specific style cannot be achieved with existing Tailwind v2 utility classes.

- **Database Management**
    - When updating the database schema use the Liquibase change logs
        - Add to the change logs and do not modify existing change logs
    - When working on database schema updates make sure it adheres to the concepts in the 'database-design-guide.md'
        - Only add table if there is no other option
        - Otherwise just update an existing table column or relationships

- **Design Principles**
    - Always apply the following design principles for every code you write:
        1. **Single Responsibility Principle (SRP)**
        2. **Open/Closed Principle (OCP)**
        3. **Liskov Substitution Principle (LSP)**
        4. **Interface Segregation Principle (ISP)**
        5. **Dependency Inversion Principle (DIP)**
        6. **Don't Repeat Yourself (DRY)**
        7. **Keep It Simple, Stupid (KISS)**
        8. **You Ain't Gonna Need It (YAGNI)**
        9. **Single Level of Abstraction (SLA)**
        10. **Law of Demeter (LoD)**

## Common Pitfalls to Avoid

Avoid these common mistakes when working on this codebase:

- **Using Hotwire Stimulus or JavaScript for business logic or complex operations**
    - Limit Stimulus to simple UI interactions (show/hide, toggle, CSS changes)
    - Avoid HTTP requests or template construction in JavaScript
    - Delegate complex logic to Spring controllers and Thymeleaf

- **Embedding complex logic or excessive Spring Expression Language in Thymeleaf templates**
    - Keep templates minimal and focused on rendering
    - Avoid complex null checks, nested if-else statements, and computations in templates
    - Controller should provide render-ready data structures to reduce Thymeleaf errors and ensure type safety.

- **Relying on generic exceptions**
    - Define specific, typed exceptions with error codes for better error handling
    - Follow the pattern in `booking/exception/` with error code enums
    - Enable proper distinction between validation errors (in-modal) and system errors (toast)

- **Neglecting the database design guide or Liquibase change logs**
    - Study `database-design-guide.md` to understand schema and relationships thoroughly
    - Always use Liquibase for schema changes; never modify existing changelogs
    - Add new changesets rather than editing historical ones

- **Overlooking existing templates and static HTML/CSS in the codebase**
    - Search for and reuse existing UI components and fragments before creating new ones
    - Check `src/main/resources/templates/fragments/` for reusable components
    - Maintain consistency with existing design patterns and CSS classes

- **Prioritizing complex solutions over simplicity**
    - Focus on the problem and choose the simplest, least intrusive solution
    - Fewer moving parts and side effects lead to more maintainable code
    - Follow KISS and YAGNI principles rigorously

- **Deviating from Spring MVC best practices**
    - Adhere to Spring MVC conventions consistently
    - Use proper separation: Controllers → Services → Repositories
    - Follow established patterns for request mapping, validation, and error handling

## Design Patterns & Architecture Notes
- **Layered MVC** – Controllers only orchestrate view models and delegate to services; services coordinate repositories, storage, and external fetchers; repositories remain thin Spring Data interfaces.
- **DTO/Form Pattern** – Every Thymeleaf form (recipes, jobs, settings) binds to a dedicated DTO (`RecipeForm`, `JobForm`, `SettingsForm`) to keep controllers decoupled from entities.
- **Strategy for Fetching** – `ScraperEngine` selects between `HtmlFetcher` and `PlaywrightFetcher` at runtime based on `SettingsService` (set in `/settings`). Fetchers implement a shared `PageFetcher` interface.
- **Template Composition** – All views compose via `layout.html` and fragments (`recipes/test-result`, `jobs/target-details`, `error.html`). HTMX handles partial swaps (e.g., recipe tests, job target drill-down) with CSRF headers injected from the layout script.
- **Quartz Scheduling** – `ScrapeJobSchedulerService` registers jobs with Quartz using `ScrapeQuartzJob`. Job metadata (`ScrapeJob`, `ScrapeTarget`, `ScrapeResultData`) persists in MySQL, while raw/processed HTML goes to the filesystem via `FileStorageService`.
- **Resilience & Observability** – Global `error.html` + specific `error/403.html` ensure user-friendly failures; controllers surface validation errors with Tailwind-styled alerts. Dev-only security initialiser updates passwords via repository-level update to avoid optimistic-lock issues.

## Testing Guidelines
- Use JUnit 5, Spring Boot test slices, and Mockito; annotate integration flows with ` @SpringBootTest`.
- Use Cucumber for BDD testing; annotate integration flows with ` @CucumberContextConfiguration`.
- Name classes `<Component>Tests` and methods `should<Expectation>`; place fixtures in `src/test/resources`.
- Cover REST endpoints, data repositories, and Liquibase migrations; fail tests on intentional contract changes.
- Regenerate REST Docs with `./mvnw test -q` and review HTML in `target/generated-docs`.
- Always append `-q` to Maven test commands (e.g., `./mvnw test -q`) so output stays focused on errors.

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

## Commit & Pull Request Guidelines
- With no existing history, follow Conventional Commits (e.g., `feat: add project scaffolding`) in imperative mood.
- Link issue IDs in the body, summarize schema/security impacts, and note config updates.
- PRs must describe scope, include test evidence (command output or screenshots), and attach curl/postman examples for API changes.
- Request at least one reviewer and keep each PR focused on a single change set.

## Environment & Configuration Notes
- Supply secrets via `.env` or shell exports; never commit real credentials—the compose sample values are dev-only.
- Align `application-*.yml` profiles with docker services, and manage schema evolution through Liquibase changelog files.
