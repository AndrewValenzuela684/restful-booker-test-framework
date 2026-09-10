# Restful Booker Test Framework

A test automation framework covering both the UI and API layers of a hotel-booking application, built with Cucumber (BDD), Selenium WebDriver, and RestAssured.

- **UI suite** — runs against [automationintesting.online](https://automationintesting.online) ("Shady Meadows B&B"), a public restful-booker-platform demo site by Mark Winteringham.
- **API suite** — runs against [restful-booker](https://restful-booker.herokuapp.com), a public hotel-booking REST API built for practicing automation.

## Tech stack

- Java 11
- Cucumber 7 (BDD-style feature files)
- Selenium WebDriver 4.9.1 (UI automation, Page Object Model)
- RestAssured 5.3.0 (API automation)
- JUnit 4 (test runners and assertions)
- Maven (build and dependency management)
- Log4j (logging)

## What's tested

### UI suite (tag `@smoke`)

| Feature | Scenario | Notes |
|---|---|---|
| Login | Valid admin login | Logs in and confirms the login form is replaced by the admin dashboard |
| Login | Invalid admin login shows an error (`@negative`) | Confirms both that an error message appears *and* that the user is still logged out |
| Book a Room | Successfully book a room | Full flow: pick a room → confirm dates → fill guest details → submit → confirm booking |
| Contact Us | Successfully submit a contact enquiry | Fills and submits the contact form, confirms the acknowledgement message |

Built with a Page Object Model (`Pages/`) — every locator was verified against the live site's real DOM rather than guessed. Waits use explicit `WebDriverWait` conditions tied to what's actually happening on the page (a form disappearing, a confirmation appearing) instead of fixed sleeps, to avoid both false failures (checking too early) and wasted time (waiting longer than necessary). Every scenario takes a screenshot on completion, pass or fail, attached to the Cucumber report via `Hooks.java`.

### API suite (tag `@api`)

| Scenario | What it covers |
|---|---|
| Create a booking | POST, verifies response body, captures the generated booking ID |
| Retrieve a booking | GET by ID, verifies the data matches what was created |
| Update a booking | PUT with an auth token (sent as a Cookie header, not `Authorization`) |
| Delete a booking | DELETE with auth — verifies the real (and non-obvious) `201` status restful-booker returns on success, then confirms with a follow-up GET returning `404` |
| Delete without authorization (`@negative`) | Attempts a delete with no auth token — confirms a `403`, and that the booking is still retrievable afterward |

Every status code and quirk here (the `201` on delete, the `403` without auth, the Cookie-vs-Authorization-header requirement) was verified directly against the live API rather than assumed.

## Project layout

```
src/test/java/
├── APIStepDefinitions/   # RestAssured step definitions (booking CRUD, auth)
├── StepDefinitions/      # Selenium step definitions + Hooks (screenshot/close-browser)
├── Pages/                # Page Object Model classes (one per page/component)
├── Utils/                # Config reader, constants, shared WebDriver helpers, test data
├── TestRunner/           # Cucumber JUnit runners
src/test/resources/
├── Features/             # Gherkin feature files
├── Config/               # config.properties
```

## Configuration

`src/test/resources/Config/config.properties`:

| Key | Purpose |
|---|---|
| `browserType` | Which local WebDriver to launch (`Chrome`, `Firefox`, `IE`, or defaults to Edge) |
| `Headless` | `true`/`false` — runs Chrome with no visible window when `true` |
| `url` | Page `@Before` navigates to at the start of every UI scenario (the admin login page) |
| `homeUrl` | Public homepage — `BookRoom`/`ContactUs` scenarios navigate here explicitly, overriding `url` |
| `username` / `password` | Valid admin credentials, used by the Login scenario |

## Running the tests

**UI smoke suite** (everything tagged `@smoke`, including both negative and positive scenarios):
- IntelliJ: right-click `TestRunner/SmokeRunner.java` → Run
- Command line: `mvn test` (Surefire is configured to run `SmokeRunner` by default)

**API suite** (everything tagged `@api`):
- IntelliJ: right-click `TestRunner/APIRunner.java` → Run
- Command line: `mvn test -Dtest=APIRunner`

**Re-running only what failed**, instead of the whole suite:
- IntelliJ: right-click `TestRunner/FailedRunner.java` → Run
- Command line: `mvn test -Dtest=FailedRunner`
- Reads `target/failed.txt`, generated automatically by the `rerun` plugin after any run — useful when a failure turns out to be environment flakiness rather than a real regression (see notes below).

Cucumber also generates an HTML report per run at `target/Cucumber.html`, with every step, timing, and screenshot embedded.

## Design notes

A few things worth calling out about how this suite was built:

- **Everything was verified live, not assumed.** Every locator, every status code, every quirky bit of API/UI behavior documented above (restful-booker's `201` on delete, the site's CSS `scroll-behavior: smooth` breaking Selenium's auto-scroll, a booking widget that silently requires `checkin`/`checkout` query params) was confirmed against the real system before being written into a test, rather than guessed from documentation or memory.
- **Negative-path coverage, not just happy paths.** Both suites include a deliberate failure case (invalid login, unauthorized delete) alongside the successful flow, and each of those asserts on *two* things — that the expected error occurred, and that the "bad" outcome didn't also sneak through.
- **Tag-based organization.** Scenarios carry more than one tag where useful (`@smoke @negative`, `@api @negative`), so the suite can be filtered by runner type or by test category independently.
- **DRY over convenience copy-paste.** Repeated locators and near-duplicate assertion logic were consolidated into shared constants (e.g. `LoginPage.USERNAME_ID`) and helper methods rather than left duplicated across files.
- **Flaky third-party dependencies are treated as a known category of problem, not a bug to chase.** The UI suite runs against a small, free public demo site with no uptime guarantees — a slow or unresponsive run doesn't mean the test (or the feature) is broken. `FailedRunner` exists specifically to cheaply re-verify a failure before assuming it's real.
