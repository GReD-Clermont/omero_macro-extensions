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

    private static final String FORMAT = "[%-55s]\t%s%-9s%s (%.3f s)";

    private long start = System.currentTimeMillis();

    private Logger logger;


    /**
     * Callback that is invoked once <em>before</em> all tests in the current container.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        //noinspection AccessOfSystemProperties
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        String klass = context.getTestClass()
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
        float time = (float) (System.currentTimeMillis() - start) / 1000;
        logStatus(context.getRequiredTestMethod().getName(), context.getDisplayName(), "DISABLED", ANSI_BLUE, time);
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
        float time = (float) (System.currentTimeMillis() - start) / 1000;
        logStatus(context.getRequiredTestMethod().getName(), context.getDisplayName(), "SUCCEEDED", ANSI_GREEN, time);
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
        float time = (float) (System.currentTimeMillis() - start) / 1000;
        logStatus(context.getRequiredTestMethod().getName(), context.getDisplayName(), "ABORTED", ANSI_YELLOW, time);
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
        float time = (float) (System.currentTimeMillis() - start) / 1000;
        logStatus(context.getRequiredTestMethod().getName(), context.getDisplayName(), "FAILED", ANSI_RED, time);
    }


    /**
     * Logs test status.
     *
     * @param methodName  The method name.
     * @param displayName The test display name.
     * @param status      The test status.
     * @param time        The time it took to run.
     */
    private void logStatus(String methodName, String displayName, String status, String color, float time) {
        displayName = displayName.equals(methodName + "()") ? "" : displayName;
        String name = String.format("%s %s", methodName, displayName);
        logger.info(String.format(FORMAT, name, color, status, ANSI_RESET, time));
    }

}
