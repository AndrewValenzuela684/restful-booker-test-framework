package Utils;

import StepDefinitions.PageInitializer;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class CommonMethods extends PageInitializer {

    public static WebDriver driver;

    public static void openBrowserAndLaunchApplication() {
        ConfigReader.readProperties();

        String browserType = ConfigReader.getPropertyValue("browserType");
        boolean headless = ConfigReader.getPropertyValue("Headless").equals("true");
        switch (browserType) {
            case "Chrome":
                // No webdriver.chrome.driver system property is set here on purpose -
                // Selenium Manager (built into Selenium 4.6+) auto-detects the installed
                // Chrome version and downloads/locates the matching driver itself. That
                // makes this work unchanged on any machine (including CI runners), instead
                // of only wherever a chromedriver.exe happens to sit at a hardcoded path.
                ChromeOptions ops = new ChromeOptions();
                ops.addArguments("--no-sandbox");
                ops.addArguments("--remote-allow-origins=*");
                if(headless){
                    ops.addArguments(("--headless=new"));
                    // window().maximize() is unreliable in headless mode - there's no real
                    // screen to maximize to, so Chrome can end up with an inconsistent window
                    // size, which throws off the scroll-into-view math for any element further
                    // down a page than the initial viewport (verified: this caused a real
                    // ElementClickInterceptedException clicking a room's "Book now" link,
                    // ~1600px down the homepage). An explicit window size avoids the guesswork.
                    ops.addArguments("--window-size=1920,1080");
                }

                driver = new ChromeDriver(ops);
                break;

            case "Firefox":
                driver = new FirefoxDriver();
                break;

            case "IE":
                driver = new InternetExplorerDriver();
                break;

            default:
                driver = new EdgeDriver();
                break;

        }

        if(!headless){
            driver.manage().window().maximize();
        }
        driver.get(ConfigReader.getPropertyValue("url"));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(Constants.WAIT_TIME));
        initializePageObjects();
        DOMConfigurator.configure("log4j.xml");
        Log.startTestCase("This is the beginning of my test case");
        Log.info("My test case is executing right now");
        Log.warning("My test case might have some trivial issues");
    }


    public static void closeBrowser() {
        if (driver == null) {
            // browser launch must have failed before this hook ran - nothing to close
            Log.warning("Skipping browser close - driver was never initialized");
            return;
        }
        Log.info("This test case is about to get completed");
        Log.endTestCase("This test case is finished");
        driver.close();
    }


    public static void doClick(WebElement element) {
        // Selenium's built-in "scroll into view before clicking" step isn't reliable on
        // pages that set CSS scroll-behavior: smooth (verified present on the live homepage)
        // - the animated scroll can still be in progress when Selenium calculates the click
        // point, so the click lands on the wrong spot and gets intercepted by whatever
        // content happens to be there instead. Forcing an instant scroll ourselves first,
        // via behavior: 'instant' (which overrides the page's own smooth-scroll CSS for this
        // call), guarantees the element is actually in place before we ever attempt to click.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
        element.click();
    }

    public static void sendText(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    public static Select clickOnDropdown(WebElement element) {
        Select select = new Select(element);
        return select;
    }

    public static void selectByValue(WebElement element, String value) {
        clickOnDropdown(element).selectByValue(value);
    }

    public static void selectByVisibleText(WebElement element, String text) {
        clickOnDropdown(element).selectByVisibleText(text);
    }

    public static void selectByIndex(WebElement element, int index) {
        clickOnDropdown(element).selectByIndex(index);
    }

    public static void selectByOptions(WebElement element, String text) {
        List<WebElement> options = clickOnDropdown(element).getOptions();
        for (WebElement option : options) {
            String ddlOptionText = option.getText();
            if (ddlOptionText.equals(text)) {
                option.click();
            }
        }
    }


    public static byte[] takeScreenshot(String imageName) {
        if (driver == null) {
            // browser launch must have failed before this hook ran (e.g. ChromeDriver
            // couldn't be located) - nothing to screenshot, and casting null would throw
            // a NullPointerException that just masks the real error above it in the log
            Log.warning("Skipping screenshot - driver was never initialized");
            return null;
        }
        // This casts the webDriver instance 'driver' to TakeScreenshot Interface
        TakesScreenshot ts = (TakesScreenshot) driver;
        byte[] picBytes = ts.getScreenshotAs(OutputType.BYTES);
        File sourcePath = ts.getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(sourcePath, new File(Constants.SCREENSHOT_FILEPATH + imageName + getTimeStamp("yyyy-MM-dd-HH-mm-ss") + ".png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return picBytes;
    }

    public static String getTimeStamp(String pattern){
        Date date = new Date();
        SimpleDateFormat sdf= new SimpleDateFormat(pattern);
        return sdf.format(date);
    }


}