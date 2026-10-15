package meridian.theme;

import java.util.Objects;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Immutable application preferences. SYSTEM follows JavaFX's typed platform preferences. */
public record MeridianPreferences(Appearance appearance, DensityMode density, double textScale, Motion motion) {
    public enum Appearance { SYSTEM, LIGHT, DARK }
    public enum Motion { SYSTEM, FULL, REDUCED }

    public MeridianPreferences {
        Objects.requireNonNull(appearance, "appearance");
        Objects.requireNonNull(density, "density");
        Objects.requireNonNull(motion, "motion");
        if (!Double.isFinite(textScale) || textScale < 1 || textScale > Float.MAX_VALUE / 40.0) {
            throw new IllegalArgumentException("Text scale must be at least 1 and keep CSS dimensions finite");
        }
    }

    public static MeridianPreferences defaults() {
        return new MeridianPreferences(Appearance.SYSTEM, DensityMode.COMFORTABLE, 1, Motion.SYSTEM);
    }

    /** Nullable optional preferences fall back independently to documented defaults. */
    public static MeridianPreferences optional(Appearance appearance, DensityMode density, Double textScale, Motion motion) {
        var defaults = defaults();
        return new MeridianPreferences(appearance == null ? defaults.appearance : appearance,
                density == null ? defaults.density : density, textScale == null ? defaults.textScale : textScale,
                motion == null ? defaults.motion : motion);
    }
}
