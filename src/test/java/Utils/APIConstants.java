package Utils;

import io.restassured.RestAssured;

public class APIConstants {

    public static final String BaseURI = RestAssured.baseURI = "https://restful-booker.herokuapp.com";

    public static final String GENERATE_TOKEN_URI = BaseURI + "/auth";
    public static final String BOOKING_URI = BaseURI + "/booking";
    // GET/PUT/DELETE for a specific booking need the id appended to BOOKING_URI,
    // e.g. BOOKING_URI + "/" + bookingId  ->  https://restful-booker.herokuapp.com/booking/123

    //creating constants for headers
    public static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_VALUE_CONTENT_TYPE = "application/json";
    // restful-booker expects the token back as a Cookie header, not Authorization: Bearer
    public static final String HEADER_KEY_COOKIE = "Cookie";

}
