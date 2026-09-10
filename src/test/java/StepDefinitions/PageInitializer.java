package StepDefinitions;

import Pages.ContactPage;
import Pages.HomePage;
import Pages.LoginPage;
import Pages.ReservationPage;

public class PageInitializer {

    public static LoginPage login;
    public static HomePage homePage;
    public static ReservationPage reservationPage;
    public static ContactPage contactPage;

    public static void initializePageObjects(){
        login = new LoginPage();
        homePage = new HomePage();
        reservationPage = new ReservationPage();
        contactPage = new ContactPage();
    }
}
