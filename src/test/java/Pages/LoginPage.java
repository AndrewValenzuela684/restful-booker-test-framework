package Pages;

import Utils.CommonMethods;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage extends CommonMethods {

    // single source of truth for this id, so the @FindBy annotation below and the
    // "has the login form disappeared/stayed" checks in Login.java can't drift out of
    // sync with each other if this ever changes
    public static final String USERNAME_ID = "username";

    public LoginPage(){
        PageFactory.initElements(driver,this);
    }

    // verified against the live admin login form at automationintesting.online/admin -
    // these fields have no "name" attribute at all, only "id" (corrected after the first
    // real test run proved *[name='username'] doesn't match anything on the live page)
    @FindBy(id = USERNAME_ID)
    public WebElement usernameTextBox;

    @FindBy(id="password")
    public WebElement passwordTextBox;

    @FindBy(id="doLogin")
    public WebElement loginBtn;
}
