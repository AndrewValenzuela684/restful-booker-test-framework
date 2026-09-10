package Pages;

import Utils.CommonMethods;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ReservationPage extends CommonMethods {

    public ReservationPage(){
        PageFactory.initElements(driver,this);
    }

    // this one button plays two roles in sequence on the live site: the first click
    // confirms the pre-selected check-in/check-out dates and reveals the guest-details
    // form underneath, the second click (after the form is filled in) actually submits
    // the reservation. Same locator both times is safe - PageFactory re-finds the element
    // fresh on each use rather than caching a stale reference.
    @FindBy(xpath = "//button[normalize-space()='Reserve Now']")
    public WebElement reserveNowBtn;

    // guest-details fields only exist in the DOM after the first Reserve Now click -
    // verified live, no id attributes on this site, only name
    @FindBy(name = "firstname")
    public WebElement firstNameInput;

    @FindBy(name = "lastname")
    public WebElement lastNameInput;

    @FindBy(name = "email")
    public WebElement emailInput;

    @FindBy(name = "phone")
    public WebElement phoneInput;
}
