# Restful Booker Test Framework

[![Run Tests](https://github.com/AndrewValenzuela684/restful-booker-test-framework/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/AndrewValenzuela684/restful-booker-test-framework/actions/workflows/tests.yml)

I built this to show my Selenium, Cucumber, and RestAssured skills. I picked two public demo targets and built out real UI and API test suites against them, end to end, including a CI pipeline that actually runs them on every push.

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
- GitHub Actions (CI — runs the full suite on every push/PR to `main`)

## What's tested

### UI suite (tag `@smoke`)

| Feature | Scenario | Notes |
|---|---|---|
| Login | Valid admin login | Logs in and confirms the login form is replaced by the admin dashboard |
| Login | Invalid admin login shows an error (`@negative`) | A `Scenario Outline` — runs against 3 different bad credential combinations (both wrong, wrong password only, wrong username only) instead of just one hardcoded case |
| Book a Room | Successfully book a room | Full flow: pick a room → confirm dates → fill guest details → submit → confirm booking |
| Contact Us | Successfully submit a contact enquiry | Fills and submits the contact form, confirms the acknowledgement message |

Locators live in a Page Object Model under `Pages/`, and I checked every one of them against the live site's actual DOM rather than guessing from a screenshot. Waits are explicit `WebDriverWait` conditions tied to something real happening on the page (a form disappearing, a confirmation showing up) instead of fixed sleeps — I'd rather wait exactly as long as needed than either fail early or waste time. Every scenario grabs a screenshot when it finishes, pass or fail, and it gets attached to the Cucumber report through `Hooks.java`.

### API suite (tag `@api`)

| Scenario | What it covers |
|---|---|
| Create a booking | POST, verifies response body, captures the generated booking ID |
| Retrieve a booking | GET by ID, verifies the data matches what was created |
| Update a booking | PUT with an auth token (sent as a Cookie header, not `Authorization`) |
| Delete a booking | DELETE with auth — verifies the real (and non-obvious) `201` status restful-booker returns on success, then confirms with a follow-up GET returning `404` |
| Delete without authorization (`@negative`) | Attempts a delete with no auth token — confirms a `403`, and that the booking is still retrievable afterward |

Same rule as the UI suite: I didn't write an assertion for any status code or quirk here until I'd actually seen it happen against the live API first.

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
.github/workflows/        # CI pipeline definition
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
- Reads `target/failed.txt`, generated automatically by the `rerun` plugin after any run — handy when a failure turns out to be environment flakiness rather than a real regression (more on that below).

Cucumber also generates an HTML report per run at `target/Cucumber.html`, with every step, timing, and screenshot embedded.

CI runs on every push and PR to `main` via GitHub Actions — see the badge at the top of this file. It runs the UI suite, retries anything that fails once with `FailedRunner`, then runs the API suite, and uploads the Cucumber report as a downloadable artifact regardless of outcome.

## A few notes on how I built this

I didn't want to write a single locator, status code, or assertion based on a guess. Everything in here — every element selector, every HTTP response code, every weird piece of behavior I documented above (restful-booker returning a `201` instead of a `200` on a successful delete, the homepage's `scroll-behavior: smooth` CSS quietly breaking Selenium's auto-scroll-into-view) — I confirmed against the real site or API first, then wrote the test around what I actually saw.

Both suites also have at least one negative test alongside the happy path, and I made a point of asserting on two things in each of those, not one: that the expected error actually showed up, *and* that the bad outcome didn't sneak through anyway (e.g., after a failed login, checking that the user is still logged out — not just that an error message appeared somewhere on the page).

The UI suite hits a small, free public demo site with no uptime guarantees, so I built in a way to tell "the site was just slow" apart from "I broke something" — that's what `FailedRunner` is for, and CI does this automatically now too. A red X in CI doesn't automatically mean the code regressed; it's worth a second look before assuming that.

A couple of smaller things I cleaned up along the way as I noticed them: locators that were duplicated across a page object and a step file got pulled into one shared constant instead of copy-pasted, and the invalid-login test went from one hardcoded bad-credentials case to a data-driven `Scenario Outline` that runs the same check against a few different combinations.
