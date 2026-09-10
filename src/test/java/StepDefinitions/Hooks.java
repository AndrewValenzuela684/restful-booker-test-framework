package StepDefinitions;

import Utils.CommonMethods;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks extends CommonMethods {

    // "not @api" - skip browser launch entirely for API scenarios, they don't need one
    @Before("not @api")
    public void preConditions() {
        openBrowserAndLaunchApplication();
    }
    // Scenario class holds the complete information of your tests execution in Cucumber framework

    @After("not @api")
    public void postConditions(Scenario scenario) {
        byte[] pic;
        if(scenario.isFailed()){
            pic=takeScreenshot("failed/" + scenario.getName());
        }else{
            pic = takeScreenshot("passed/" + scenario.getName());
        }

        //attach the screenshots in my report (skip if the browser never launched,
        //in which case takeScreenshot() already returned null - attaching null
        //throws its own NullPointerException and just hides the real error above it)
        if (pic != null) {
            scenario.attach(pic, "image/png", scenario.getName());
        }
        closeBrowser();
    }
}
