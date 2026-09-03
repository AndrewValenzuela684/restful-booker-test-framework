package APIStepDefinitions;

import Utils.APIConstants;
import Utils.APIPayloadConstants;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class APIWorkflowSteps {

    RequestSpecification request;
    Response response;
    public static String booking_id;

    @Given("a request is prepared to create a booking")
    public void a_request_is_prepared_to_create_a_booking() {
        // create is open on restful-booker, no auth header needed here
        request = given().
                header(APIConstants.HEADER_KEY_CONTENT_TYPE, APIConstants.HEADER_VALUE_CONTENT_TYPE).
                body(APIPayloadConstants.createBookingPayloadJson());
    }

    @When("a POST call is made to create a booking")
    public void a_post_call_is_made_to_create_a_booking() {
        response = request.when().post(APIConstants.BOOKING_URI);
    }

    @Then("the status code for creating a booking is {int}")
    public void the_status_code_for_creating_a_booking_is(Integer int1) {
        response.prettyPrint();
        response.then().assertThat().statusCode(int1);
    }

    @Then("the booking contains firstname {string} and lastname {string}")
    public void the_booking_contains_firstname_and_lastname(String firstname, String lastname) {
        // create's response nests the booking data under a "booking" object,
        // alongside a top-level "bookingid" - different shape than a get-by-id response
        response.then().assertThat().
                body("booking.firstname", equalTo(firstname)).
                body("booking.lastname", equalTo(lastname));
    }

    @Then("the booking id is stored as a global variable to be used for other calls")
    public void the_booking_id_is_stored_as_a_global_variable_to_be_used_for_other_calls() {
        // bookingid sits at the top level of the create response, not nested inside "booking"
        booking_id = response.jsonPath().getString("bookingid");
        System.out.println(booking_id);
    }


    @Given("a request is prepared to get the created booking")
    public void a_request_is_prepared_to_get_the_created_booking() {
        // get is also open, no auth needed - and the id goes in the URL path, not a query param
        request = given().header(APIConstants.HEADER_KEY_CONTENT_TYPE,
                APIConstants.HEADER_VALUE_CONTENT_TYPE);
    }

    @When("a GET call is made to get the booking")
    public void a_get_call_is_made_to_get_the_booking() {
        response = request.when().get(APIConstants.BOOKING_URI + "/" + booking_id);
    }

    @Then("the status code for this booking is {int}")
    public void the_status_code_for_this_booking_is(Integer int1) {
        response.then().assertThat().statusCode(int1);
    }

    @Then("the retrieved booking matches the data of the created booking")
    public void the_retrieved_booking_matches_the_data_of_the_created_booking(DataTable dataTable) {
        // unlike the create response, GET /booking/{id} returns the booking fields directly,
        // with no "booking" wrapper and no bookingid in the body
        List<Map<String, String>> expectedData = dataTable.asMaps();

        for (Map<String, String> map : expectedData) {
            Set<String> keys = map.keySet();
            for (String key : keys) {
                String expectedValue = map.get(key);
                String actualValue = response.jsonPath().getString(key);
                Assert.assertEquals(expectedValue, actualValue);
            }
        }
    }
    //------------------------------------------------------------------------------------------

    @Given("a request is prepared to update the booking")
    public void a_request_is_prepared_to_update_the_booking() {
        // update DOES require auth - token goes in a Cookie header, not Authorization
        request = given().header(APIConstants.HEADER_KEY_CONTENT_TYPE,
                        APIConstants.HEADER_VALUE_CONTENT_TYPE).
                header(APIConstants.HEADER_KEY_COOKIE, "token=" + GenerateTokenSteps.token).
                body(APIPayloadConstants.updateBookingPayloadJson());
    }

    @When("a PUT call is made to update the booking")
    public void a_put_call_is_made_to_update_the_booking() {
        response = request.when().put(APIConstants.BOOKING_URI + "/" + booking_id);
    }

    @Then("the status code of updated booking is {int}")
    public void the_status_code_of_updated_booking_is(Integer int1) {
        response.then().assertThat().statusCode(int1);
    }
}
