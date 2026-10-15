package meridian.theme.metrics;

/** Generated from tokens/meridian.json; source SHA-256 1c14eaf53bc0801d37c190a16a1abf3a91820502ee58a238028a2f8a20d81d25. Logical pixels, never hard ceilings. */
public final class MeridianMetrics {
    private MeridianMetrics() {}
    public enum DensityMode { COMFORTABLE, COMPACT }
    public record Typography(double general, double table, double metadata, double section, double title) {
        public Typography {
            for (double value : new double[] {general, table, metadata, section, title}) positive(value);
            if (general < 14 || table < 13 || metadata < 12 || section < 12 || title < 12) throw new IllegalArgumentException("Typography floor");
        }
        public Typography scaledForText(double scale) {
            textScale(scale);
            return new Typography(general * scale, table * scale, metadata * scale, section * scale, title * scale);
        }
    }
    public record Geometry(double controlRadius, double popupRadius, double dialogRadius, double divider, double resizeHitReach, double iconSize) {
        public Geometry {
            for (double value : new double[] {controlRadius, popupRadius, dialogRadius, divider, resizeHitReach, iconSize}) positive(value);
        }
    }
    public record DensityMetrics(double controlMinHeight, double tableRowMinHeight, double headerMinHeight, double iconTargetMinSize, double horizontalPadding, double verticalPadding) {
        public DensityMetrics {
            for (double value : new double[] {controlMinHeight, tableRowMinHeight, headerMinHeight, iconTargetMinSize, horizontalPadding, verticalPadding}) positive(value);
        }
        public DensityMetrics scaledForText(double scale) {
            textScale(scale);
            return new DensityMetrics(controlMinHeight * scale, tableRowMinHeight * scale, headerMinHeight * scale, iconTargetMinSize * scale, horizontalPadding, verticalPadding);
        }
        public double controlHeightFor(double measuredContentHeight) {
            nonnegative(measuredContentHeight);
            return Math.max(controlMinHeight, measuredContentHeight);
        }
        public double tableRowHeightFor(double measuredContentHeight) {
            nonnegative(measuredContentHeight);
            return Math.max(tableRowMinHeight, measuredContentHeight);
        }
        public double headerHeightFor(double measuredContentHeight) {
            nonnegative(measuredContentHeight);
            return Math.max(headerMinHeight, measuredContentHeight);
        }
    }
    public record Motion(int hoverMs, int overlayMs, int panelMs) {
        public Motion {
            if (hoverMs < 0 || overlayMs < 0 || panelMs < 0) throw new IllegalArgumentException("Negative duration");
        }
        public Motion reduced() { return new Motion(0, 0, 0); }
    }
    public static final Typography TYPOGRAPHY = new Typography(14.0, 13.0, 12.0, 16.0, 25.0);
    public static final Geometry GEOMETRY = new Geometry(6.0, 8.0, 12.0, 1.0, 12.0, 16.0);
    public static final DensityMetrics COMFORTABLE = new DensityMetrics(32.0, 40.0, 32.0, 32.0, 10.0, 4.0);
    public static final DensityMetrics COMPACT = new DensityMetrics(28.0, 32.0, 28.0, 28.0, 8.0, 2.0);
    public static final Motion MOTION = new Motion(110, 150, 180);
    public static DensityMetrics density(DensityMode mode) {
        return switch (java.util.Objects.requireNonNull(mode, "density")) {
            case COMFORTABLE -> COMFORTABLE;
            case COMPACT -> COMPACT;
        };
    }
    private static void positive(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Expected finite positive metric");
    }
    private static void nonnegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Expected finite nonnegative height");
    }
    private static void textScale(double value) {
        if (!Double.isFinite(value) || value < 1) throw new IllegalArgumentException("Text scale must be at least 1");
    }
}
