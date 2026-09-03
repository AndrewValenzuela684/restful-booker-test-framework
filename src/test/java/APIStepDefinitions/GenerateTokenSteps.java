package APIStepDefinitions;

import Utils.APIConstants;
import io.cucumber.java.en.Given;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class GenerateTokenSteps {

    public static String token;

    @Given("a JWT is generated")
    public void a_jwt_is_generated() {
        RequestSpecification generateTokenRequest = given()
                .header(APIConstants.HEADER_KEY_CONTENT_TYPE, APIConstants.HEADER_VALUE_CONTENT_TYPE)
                .body("{\n" +
                        "    \"username\" : \"admin\",\n" +
                        "    \"password\" : \"password123\"\n" +
                        "}");

        // hitting the /auth endpoint
        Response response = generateTokenRequest.when().post(APIConstants.GENERATE_TOKEN_URI);

        // restful-booker returns { "token": "abc123..." }
        // stored raw, no "Bearer " prefix - it gets sent back later as a Cookie header,
        // not an Authorization header, so the prefix would just be wrong data in the cookie
        token = response.jsonPath().getString("token");
        System.out.println(token);
    }
}
