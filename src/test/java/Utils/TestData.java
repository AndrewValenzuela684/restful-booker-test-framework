package Utils;

import java.util.Random;

public class TestData {

    // shared random-name pools, used anywhere a scenario needs a made-up guest name
    // instead of a hardcoded one (currently BookRoom and ContactUs)
    private static final String[] FIRST_NAMES = {"Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley"};
    private static final String[] LAST_NAMES = {"Bennett", "Carter", "Diaz", "Foster", "Nguyen", "Ramirez"};
    private static final Random RANDOM = new Random();

    public static String randomFirstName() {
        return FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
    }

    public static String randomLastName() {
        return LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
    }
}
