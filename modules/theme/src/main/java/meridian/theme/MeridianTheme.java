package meridian.theme;

import atlantafx.base.theme.Theme;
import java.util.HashSet;
import java.util.Set;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Four compiled AtlantaFX-derived user-agent themes; no overlay on Modena. */
public enum MeridianTheme implements Theme {
    LIGHT_COMFORTABLE(false, DensityMode.COMFORTABLE),
    LIGHT_COMPACT(false, DensityMode.COMPACT),
    DARK_COMFORTABLE(true, DensityMode.COMFORTABLE),
    DARK_COMPACT(true, DensityMode.COMPACT);

    private final boolean dark;
    private final DensityMode density;

    MeridianTheme(boolean dark, DensityMode density) {
        this.dark = dark;
        this.density = density;
    }

    public static MeridianTheme of(boolean dark, DensityMode density) {
        java.util.Objects.requireNonNull(density, "density");
        return dark ? (density == DensityMode.COMPACT ? DARK_COMPACT : DARK_COMFORTABLE)
                : (density == DensityMode.COMPACT ? LIGHT_COMPACT : LIGHT_COMFORTABLE);
    }

    @Override public String getName() { return "Meridian " + (dark ? "Dark " : "Light ") + density.name().toLowerCase(java.util.Locale.ROOT); }
    @Override public boolean isDarkMode() { return dark; }
    public DensityMode density() { return density; }
    @Override public String getUserAgentStylesheet() {
        return "/meridian/theme/themes/meridian-" + (dark ? "dark" : "light") + "-"
                + density.name().toLowerCase(java.util.Locale.ROOT) + ".css";
    }
    @Override public String getUserAgentStylesheetBSS() { return null; }
    @Override public String getUserAgentStylesheet(Set<String> modules) {
        if (modules == null || modules.isEmpty()) return getUserAgentStylesheet();
        var required = new HashSet<>(modules);
        required.add("meridian");
        return Theme.super.getUserAgentStylesheet(required);
    }
}
