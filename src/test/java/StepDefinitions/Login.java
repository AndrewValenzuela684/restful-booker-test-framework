package StepDefinitions;

import Pages.LoginPage;
import Utils.CommonMethods;
import Utils.ConfigReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Login extends CommonMethods {


    @Given("open the browser and launch HRMS application")
    public void open_the_browser_and_launch_hrms_application() {

        openBrowserAndLaunchApplication();

    }


    @When("user enters valid email and valid password")
    public void user_enters_valid_email_and_valid_password() {

        sendText(login.usernameTextBox, ConfigReader.getPropertyValue("username"));
        sendText(login.passwordTextBox, ConfigReader.getPropertyValue("password"));

    }

    @When("click on login button")
    public void click_on_login_button() {

        doClick(login.loginBtn);

    }

    @Then("user is logged in successfully into the application")
    public void user_is_logged_in_successfully() {
        // clicking login triggers a real network request before the page navigates to
        // /admin/rooms - checking the instant this step starts can race ahead of that, so
        // wait explicitly for the login form to actually disappear rather than assuming
        // it's already gone (verified for real: a successful login does remove id="username"
        // and lands on /admin/rooms, showing the room management table)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean loginFormGone = wait.until(d -> d.findElements(By.id(LoginPage.USERNAME_ID)).isEmpty());
        Assert.assertTrue("Expected the login form to be gone after logging in, but it's still showing",
                loginFormGone);
    }

    @When("user enters {string} and {string}")
    public void user_enters_username_and_password(String username, String password) {
        // {string}/{string} pulls each row straight from the Examples table in
        // Login.feature - no hardcoded values here since the whole point of a Scenario
        // Outline is running the same steps against multiple data sets. Every combination
        // in that table was verified live against the real site before being added,
        // confirming each one actually produces the "Invalid credentials" error rather
        // than being assumed.
        sendText(login.usernameTextBox, username);
        sendText(login.passwordTextBox, password);
    }

    @Then("an invalid credentials error is shown")
    public void an_invalid_credentials_error_is_shown() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean errorShown = wait.until(d ->
                !d.findElements(By.xpath("//div[normalize-space()='Invalid credentials']")).isEmpty());
        Assert.assertTrue("Expected an 'Invalid credentials' error to be showing, but it wasn't",
                errorShown);

        // a good negative test checks two things, not just one: that the expected error
        // showed up, AND that the bad outcome (getting logged in anyway) did NOT happen.
        // the login form disappearing here would mean bad credentials somehow still worked.
        boolean stillOnLoginForm = !driver.findElements(By.id(LoginPage.USERNAME_ID)).isEmpty();
        Assert.assertTrue("Expected to still be on the login form after a failed login, but it was gone",
                stillOnLoginForm);
    }

}
