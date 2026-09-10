Feature: Book a Room

  @smoke
  Scenario: Successfully book a room
    Given user is on the Shady Meadows homepage
    When user selects a room to book
    And user confirms the pre-selected dates
    And user completes the guest details form
    And user submits the reservation
    Then the booking is confirmed
