package meridian.workbench;

import java.util.Locale;
import java.util.Objects;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Noninteractive status dot and visible label. Domain states remain application-owned.
 * The inherited text property supports localization/binding and is the accessible text.
 * Styles: meridian-status, status-neutral/progress/success/warning/danger, status-dot.
 * Progress indicates an ongoing state; it does not animate or report numeric completion.
 */
public final class StatusIndicator extends Label {
    public enum Status {
        NEUTRAL, PROGRESS, SUCCESS, WARNING, DANGER;
        public String styleClass() { return "status-" + name().toLowerCase(Locale.ROOT); }
    }

    private Status status;

    public StatusIndicator(String text, Status status) {
        super(Objects.requireNonNull(text, "text"));
        getStyleClass().add("meridian-status");
        Region dot = new Region();
        dot.getStyleClass().add("status-dot");
        dot.setMouseTransparent(true);
        dot.setFocusTraversable(false);
        setGraphic(dot);
        setFocusTraversable(false);
        setAccessibleRole(AccessibleRole.TEXT);
        accessibleTextProperty().bind(textProperty());
        setStatus(status);
    }

    public Status getStatus() { return status; }

    /** Changes the semantic class without removing unrelated host classes. */
    public void setStatus(Status value) {
        Objects.requireNonNull(value, "status");
        if (value == status) return;
        if (status != null) getStyleClass().remove(status.styleClass());
        status = value;
        if (!getStyleClass().contains(value.styleClass())) getStyleClass().add(value.styleClass());
    }
}
