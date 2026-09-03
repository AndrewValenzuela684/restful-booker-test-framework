# Restful Booker Test Framework

A BDD test automation framework combining **Cucumber**, **Selenium WebDriver**, and **RestAssured** to test both the UI and API layers of a web application, currently targeting the public [restful-booker](https://restful-booker.herokuapp.com/) hotel-booking API.

## Tech stack

- Java 11
- Cucumber (BDD)
- Selenium WebDriver 4.9.1
- RestAssured 5.3.0
- JUnit / TestNG
- Maven
- Log4j
- Apache POI (Excel-driven test data)

## What's covered

**API suite** (`@api` tag)
- Token-based authentication (`POST /auth`)
- Create, retrieve, and update a booking (`POST` / `GET` / `PUT /booking`)
- Dynamic test data captured from one response and reused in the next call
- JSON response validation with RestAssured/Hamcrest matchers

**UI suite**
- Selenium WebDriver automation using the Page Object Model
- Screenshot capture on pass/fail, attached to the Cucumber report

## Project structure

```
src/test/java/
├── APIStepDefinitions/   # RestAssured step definitions (API suite)
├── StepDefinitions/      # Selenium step definitions + Hooks (UI suite)
├── Pages/                # Page Object Model classes
├── Utils/                # Config reader, constants, common methods, logging
├── TestRunner/           # JUnit + Cucumber runner classes, tag-filtered
src/test/resources/
├── Features/             # Gherkin feature files
├── Config/                # config.properties
```

## Running the tests

Run the whole API suite:

```bash
mvn test -Dcucumber.filter.tags="@api"
```

Or run a specific runner class (`APIRunner`, `SmokeRunner`) directly from IntelliJ.

## Notes

This framework was originally built during a QA automation bootcamp, targeting a private training environment provided by the course. After that environment was decommissioned, the API suite was retargeted to the public restful-booker API — remapping authentication, endpoints, and payload shapes — to keep the tests runnable and demonstrate the framework independent of any specific backend.
