package Utils;

import org.json.JSONObject;

public class APIPayloadConstants {
    //builds the JSON bodies for booking create/update calls

    public static String createBookingPayloadJson(){
        JSONObject bookingDates = new JSONObject();
        bookingDates.put("checkin", "2024-01-01");
        bookingDates.put("checkout", "2024-01-05");

        JSONObject obj = new JSONObject();
        obj.put("firstname", "nelena");
        obj.put("lastname", "faria");
        obj.put("totalprice", 150);
        obj.put("depositpaid", true);
        obj.put("bookingdates", bookingDates);
        obj.put("additionalneeds", "Breakfast");
        return obj.toString();
    }

    public static String createBookingPayloadDynamic
            (String firstname, String lastname, int totalprice,
             boolean depositpaid, String checkin, String checkout, String additionalneeds){
        JSONObject bookingDates = new JSONObject();
        bookingDates.put("checkin", checkin);
        bookingDates.put("checkout", checkout);

        JSONObject obj = new JSONObject();
        obj.put("firstname", firstname);
        obj.put("lastname", lastname);
        obj.put("totalprice", totalprice);
        obj.put("depositpaid", depositpaid);
        obj.put("bookingdates", bookingDates);
        obj.put("additionalneeds", additionalneeds);
        return obj.toString();
    }

    public static String updateBookingPayloadJson(){
        JSONObject bookingDates = new JSONObject();
        bookingDates.put("checkin", "2024-02-01");
        bookingDates.put("checkout", "2024-02-10");

        JSONObject obj = new JSONObject();
        obj.put("firstname", "natalia");
        obj.put("lastname", "glusco");
        obj.put("totalprice", 300);
        obj.put("depositpaid", false);
        obj.put("bookingdates", bookingDates);
        obj.put("additionalneeds", "Late checkout");
        return obj.toString();
    }
}
