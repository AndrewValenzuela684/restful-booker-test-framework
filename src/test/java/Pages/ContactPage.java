package Pages;

import Utils.CommonMethods;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ContactPage extends CommonMethods {

    public ContactPage(){
        PageFactory.initElements(driver,this);
    }

    // verified live against the Contact section of the homepage - unlike the reservation
    // form, these fields all have clean id attributes, no name attribute
    @FindBy(id = "name")
    public WebElement nameInput;

    @FindBy(id = "email")
    public WebElement emailInput;

    @FindBy(id = "phone")
    public WebElement phoneInput;

    @FindBy(id = "subject")
    public WebElement subjectInput;

    @FindBy(id = "description")
    public WebElement descriptionInput;

    // only one "Submit" button exists anywhere on the homepage, so this is unambiguous
    @FindBy(xpath = "//button[normalize-space()='Submit']")
    public WebElement submitBtn;
}
