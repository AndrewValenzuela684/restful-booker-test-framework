Feature: Login Functionalities

  @smoke
  Scenario: Valid Admin login
    When user enters valid email and valid password
    And click on login button
    Then user is logged in successfully into the application

  @smoke @negative
  Scenario: Invalid Admin login shows an error
    When user enters invalid email and invalid password
    And click on login button
    Then an invalid credentials error is shown
