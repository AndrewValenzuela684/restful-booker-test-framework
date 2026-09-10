Feature: API workflow for booking

  Background:
    Given a JWT is generated

  @api
  Scenario: create a booking using API call
    Given a request is prepared to create a booking
    When a POST call is made to create a booking
    Then the status code for creating a booking is 200
    Then the booking contains firstname "nelena" and lastname "faria"
    Then the booking id is stored as a global variable to be used for other calls

  @api
  Scenario: retrieve a booking using API call
    Given a request is prepared to get the created booking
    When a GET call is made to get the booking
    Then the status code for this booking is 200
    Then the retrieved booking matches the data of the created booking
      | firstname | lastname | totalprice | depositpaid | additionalneeds |
      | nelena    | faria    | 150        | true         | Breakfast |

  @api
  Scenario: update a booking using API call
    Given a request is prepared to update the booking
    When a PUT call is made to update the booking
    Then the status code of updated booking is 200

  @api
  Scenario: delete a booking using API call
    Given a request is prepared to delete the booking
    When a DELETE call is made to delete the booking
    Then the status code for deleting a booking is 201
    Then a GET call for the deleted booking returns 404

  @api @negative
  Scenario: delete a booking without authorization is rejected
    Given a request is prepared to create a booking
    When a POST call is made to create a booking
    Then the booking id is stored as a global variable to be used for other calls
    Given a request is prepared to delete the booking without authorization
    When a DELETE call is made to delete the booking
    Then the status code for deleting a booking is 403
    Then the booking still exists with status 200