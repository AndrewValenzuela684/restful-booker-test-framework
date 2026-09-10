package Pages;

import Utils.CommonMethods;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class HomePage extends CommonMethods {

    public HomePage(){
        PageFactory.initElements(driver,this);
    }

    // verified against the live homepage at automationintesting.online - each room card's
    // "Book now" link is the only element on the page whose href actually starts with
    // "/reservation/1" (the hero "Book Now" button near the top just scrolls down to
    // #booking, so this locator can't accidentally match that one instead)
    @FindBy(xpath = "//a[starts-with(@href,'/reservation/1')]")
    public WebElement bookRoom1Link;
}
