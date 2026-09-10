package StepDefinitions;

import Utils.CommonMethods;
import Utils.TestData;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ContactUs extends CommonMethods {

    // reuses BookRoom.java's "Given user is on the Shady Meadows homepage" step - Cucumber
    // matches step text across every step-definition class in the glue package, so a step
    // doesn't need to be redefined here just because it lives in another feature too

    @When("user fills out the contact form")
    public void user_fills_out_the_contact_form() {
        sendText(contactPage.nameInput, TestData.randomFirstName() + " " + TestData.randomLastName());
        sendText(contactPage.emailInput, "andrew.qa.test@example.com");
        sendText(contactPage.phoneInput, "01234567890");
        sendText(contactPage.subjectInput, "Question about my upcoming stay");
        sendText(contactPage.descriptionInput,
                "Hello, I would like to know if early check-in is possible for my reservation next month. Thank you!");
    }

    @When("user submits the contact form")
    public void user_submits_the_contact_form() {
        doClick(contactPage.submitBtn);
    }

    @Then("the contact enquiry is confirmed")
    public void the_contact_enquiry_is_confirmed() {
        // same reasoning as the other two confirmation checks - submitting fires a real
        // request before the confirmation heading swaps in, so wait for it rather than
        // assuming it's already there. Matching on a prefix rather than the full sentence
        // since the guest name (and therefore the full heading text) is randomized above.
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean confirmationShown = wait.until(d ->
                !d.findElements(By.xpath("//h3[starts-with(normalize-space(),'Thanks for getting in touch')]")).isEmpty());
        Assert.assertTrue("Expected the contact confirmation message to be showing, but it wasn't",
                confirmationShown);
    }
}
