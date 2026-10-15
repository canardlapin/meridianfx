package meridian.theme;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

/**
 * Initial upstream theme used to verify the pinned foundation.
 * Meridian's authored themes and appearance API are delivered in A2/A3.
 */
public final class FoundationTheme {
    private FoundationTheme() {}

    public static String stylesheet() {
        return new PrimerLight().getUserAgentStylesheet();
    }

    public static void apply() {
        Application.setUserAgentStylesheet(stylesheet());
    }
}
