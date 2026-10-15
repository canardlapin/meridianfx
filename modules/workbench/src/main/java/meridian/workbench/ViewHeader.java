package meridian.workbench;

import java.util.Objects;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Minimal Java-friendly view identity composition.
 * Appearance is supplied by the theme; location and docking chrome remain separate.
 */
public final class ViewHeader extends VBox {
    private final Label title = new Label();
    private final Label subtitle = new Label();

    public ViewHeader(String title, String subtitle) {
        super(12);
        getStyleClass().add("meridian-view-header");
        this.title.getStyleClass().add("meridian-view-title");
        this.subtitle.getStyleClass().add("meridian-view-subtitle");
        getChildren().addAll(this.title, this.subtitle);
        setTitle(title);
        setSubtitle(subtitle);
    }

    public String getTitle() {
        return title.getText();
    }

    public void setTitle(String value) {
        title.setText(Objects.requireNonNull(value, "title"));
    }

    public String getSubtitle() {
        return subtitle.getText();
    }

    public void setSubtitle(String value) {
        subtitle.setText(Objects.requireNonNull(value, "subtitle"));
        boolean visible = !value.isBlank();
        subtitle.setVisible(visible);
        subtitle.setManaged(visible);
    }
}
