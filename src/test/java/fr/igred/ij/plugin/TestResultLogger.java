package fr.igred.ij.plugin;


import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;
import java.util.logging.Logger;


public class TestResultLogger implements TestWatcher, BeforeTestExecutionCallback, BeforeAllCallback {

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE   = "\u001B[34m";

    private static final String FORMAT = "[%-25s]\t%-40s\t%s (%.3f s)";

    private long start = System.currentTimeMillis();

    private Logger logger;


    /**
     * Callback that is invoked once <em>before</em> all tests in the current container.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        final String klass = context.getTestClass()
                                    .orElse(context.getRequiredTestClass())
                                    .getName();
        logger = Logger.getLogger(klass);
    }


    /**
     * Callback that is invoked <em>immediately before</em> an individual test is executed but after any user-defined
     * setup methods have been executed for that test.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        start = System.currentTimeMillis();
    }


    /**
     * Invoked after a disabled test has been skipped.
     *
     * <p>The default implementation does nothing. Concrete implementations can
     * override this method as appropriate.
     *
     * @param context the current extension context; never {@code null}
     * @param reason  the reason the test is disabled; never {@code null} but
     */
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        float  time        = (float) (System.currentTimeMillis() - start) / 1000;
        String testName    = context.getTestMethod().orElse(context.getRequiredTestMethod()).getName();
        String displayName = context.getDisplayName();
        String status      = ANSI_BLUE + "DISABLED" + ANSI_RESET;
        logger.info(String.format(FORMAT, testName, displayName, status, time));
    }


    /**
     * Invoked after a test has completed successfully.
     *
     * <p>The default implementation does nothing. Concrete implementations can
     * override this method as appropriate.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void testSuccessful(ExtensionContext context) {
        float  time        = (float) (System.currentTimeMillis() - start) / 1000;
        String testName    = context.getTestMethod().orElse(context.getRequiredTestMethod()).getName();
        String displayName = context.getDisplayName();
        displayName = displayName.equals(testName + "()") ? "" : displayName;
        String status = ANSI_GREEN + "SUCCEEDED" + ANSI_RESET;
        logger.info(String.format(FORMAT, testName, displayName, status, time));
    }


    /**
     * Invoked after a test has been aborted.
     *
     * <p>The default implementation does nothing. Concrete implementations can
     * override this method as appropriate.
     *
     * @param context the current extension context; never {@code null}
     * @param cause   the throwable responsible for the test being aborted; may be {@code null}
     */
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        float  time        = (float) (System.currentTimeMillis() - start) / 1000;
        String testName    = context.getTestMethod().orElse(context.getRequiredTestMethod()).getName();
        String displayName = context.getDisplayName();
        displayName = displayName.equals(testName + "()") ? "" : displayName;
        String status = ANSI_YELLOW + "ABORTED" + ANSI_RESET;
        logger.info(String.format(FORMAT, testName, displayName, status, time));
    }


    /**
     * Invoked after a test has failed.
     *
     * <p>The default implementation does nothing. Concrete implementations can
     * override this method as appropriate.
     *
     * @param context the current extension context; never {@code null}
     * @param cause   the throwable that caused test failure; may be {@code null}
     */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        float  time        = (float) (System.currentTimeMillis() - start) / 1000;
        String testName    = context.getTestMethod().orElse(context.getRequiredTestMethod()).getName();
        String displayName = context.getDisplayName();
        displayName = displayName.equals(testName + "()") ? "" : displayName;
        String status = ANSI_RED + "FAILED" + ANSI_RESET;
        logger.info(String.format(FORMAT, testName, displayName, status, time));
    }

}
