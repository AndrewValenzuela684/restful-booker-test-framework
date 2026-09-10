Feature: Contact Us

  @smoke
  Scenario: Successfully submit a contact enquiry
    Given user is on the Shady Meadows homepage
    When user fills out the contact form
    And user submits the contact form
    Then the contact enquiry is confirmed
