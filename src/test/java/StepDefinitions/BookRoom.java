package StepDefinitions;

import Utils.CommonMethods;
import Utils.ConfigReader;
import Utils.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BookRoom extends CommonMethods {

    @Given("user is on the Shady Meadows homepage")
    public void user_is_on_the_shady_meadows_homepage() {
        // @Before already opened the browser and navigated to config's "url" (the admin
        // login page) - this scenario needs the public homepage instead, so send the
        // already-open browser there directly
        driver.get(ConfigReader.getPropertyValue("homeUrl"));
    }

    @When("user selects a room to book")
    public void user_selects_a_room_to_book() {
        doClick(homePage.bookRoom1Link);
    }

    @When("user confirms the pre-selected dates")
    public void user_confirms_the_pre_selected_dates() {
        doClick(reservationPage.reserveNowBtn);
    }

    @When("user completes the guest details form")
    public void user_completes_the_guest_details_form() {
        sendText(reservationPage.firstNameInput, TestData.randomFirstName());
        sendText(reservationPage.lastNameInput, TestData.randomLastName());
        sendText(reservationPage.emailInput, "andrew.qa.test@example.com");
        sendText(reservationPage.phoneInput, "01234567890");
    }

    @When("user submits the reservation")
    public void user_submits_the_reservation() {
        doClick(reservationPage.reserveNowBtn);
    }

    @Then("the booking is confirmed")
    public void the_booking_is_confirmed() {
        // submitting triggers a real request before the "Booking Confirmed" panel swaps
        // in, so wait rather than assuming it's already there the instant this step starts
        // (same reasoning as the login form check in Login.java)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean confirmationShown = wait.until(d ->
                !d.findElements(By.xpath("//h2[normalize-space()='Booking Confirmed']")).isEmpty());
        Assert.assertTrue("Expected the 'Booking Confirmed' message to be showing, but it wasn't",
                confirmationShown);
    }
}
