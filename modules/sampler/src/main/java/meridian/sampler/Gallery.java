package meridian.sampler;

import atlantafx.base.controls.Message;
import atlantafx.base.controls.Notification;
import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/**
 * Fixed visual-review specimens: controls, data views, an application shell, and overlays rendered
 * natively at 2x into {@code <name>-full.png} and a detail crop {@code <name>-fold.png}.
 * Content is frozen for comparison; synthetic data only.
 */
public final class Gallery {
    static final double W = 1280, H = 820, SCALE = 2;
    private static Throwable failure;
    private Gallery() {}

    public static void main(String[] args) {
        Application.launch(Window_.class, args);
        if (failure != null) throw new IllegalStateException("Gallery capture failed", failure);
    }

    record Shot(String name, String board, boolean dark, DensityMode density) {}

    static final List<Shot> SHOTS = List.of(
            new Shot("controls-light", "controls", false, DensityMode.COMFORTABLE),
            new Shot("controls-dark", "controls", true, DensityMode.COMFORTABLE),
            new Shot("data-light", "data", false, DensityMode.COMFORTABLE),
            new Shot("data-dark", "data", true, DensityMode.COMFORTABLE),
            new Shot("app-light", "app", false, DensityMode.COMFORTABLE),
            new Shot("app-dark", "app", true, DensityMode.COMFORTABLE),
            new Shot("app-dark-compact", "app", true, DensityMode.COMPACT),
            new Shot("overlays-light", "overlays", false, DensityMode.COMFORTABLE),
            new Shot("overlays-dark", "overlays", true, DensityMode.COMFORTABLE));

    public static final class Window_ extends Application {
        private Stage stage;
        private Path output;
        private MeridianAppearance binding;
        private final Deque<Step> steps = new ArrayDeque<>();
        private final List<String> written = new ArrayList<>();
        private final List<String> focusLog = new ArrayList<>();

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                if (getParameters().getRaw().size() != 1) throw new IllegalArgumentException("Pass a new output directory");
                output = Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
                if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
                Files.createDirectories(output);
                binding = MeridianAppearance.install(new MeridianPreferences(Appearance.LIGHT, DensityMode.COMFORTABLE, 1, Motion.REDUCED));
                Scene scene = new Scene(new StackPane(), W, H);
                binding.register(scene);
                stage.setScene(scene);
                stage.setTitle("MeridianFX — gallery specimens");
                stage.setResizable(false);
                stage.show();
                for (Shot shot : SHOTS) plan(shot);
                steps.add(this::finish);
                next();
            } catch (Throwable error) { fail(error); }
        }

        private void plan(Shot shot) {
            Overlays[] overlays = new Overlays[1];
            steps.add(() -> {
                binding.setPreferences(new MeridianPreferences(shot.dark ? Appearance.DARK : Appearance.LIGHT, shot.density, 1, Motion.REDUCED));
                String mode = (shot.dark ? "dark" : "light") + " · " + shot.density.name().toLowerCase(java.util.Locale.ROOT);
                Parent root = switch (shot.board) {
                    case "controls" -> Boards.controls(mode);
                    case "data" -> Boards.data(mode);
                    case "app" -> Boards.app(mode);
                    case "overlays" -> (overlays[0] = new Overlays(mode)).root;
                    default -> throw new IllegalArgumentException(shot.board);
                };
                // Capture fidelity: the real pointer must never produce hover states, and boards that
                // outgrow the window must paint their own background over the full snapshot.
                root.setMouseTransparent(true);
                stage.getScene().setRoot(root);
                Boards.forceStates(root);
            });
            if (shot.board.equals("overlays")) {
                steps.add(() -> overlays[0].openContext(stage));
                steps.add(() -> overlays[0].capture());
                steps.add(() -> overlays[0].openCombo());
                steps.add(() -> overlays[0].capture());
                steps.add(() -> overlays[0].openDate());
                steps.add(() -> overlays[0].capture());
                steps.add(() -> overlays[0].openMenuBar());
                steps.add(() -> overlays[0].capture());
                steps.add(() -> overlays[0].openTooltip(stage));
                steps.add(() -> overlays[0].capture());
                steps.add(() -> overlays[0].place());
            }
            steps.add(() -> {
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                if (root instanceof Region region) region.resize(W, Math.max(H, region.prefHeight(W)));
                // Capture fidelity: :focused also depends on OS window activation, so live-focus
                // specimens are re-focused and their focused state pinned immediately before capture.
                stage.toFront();
                stage.requestFocus();
                boolean windowFocused = stage.isFocused();
                focusLog.add(shot.name + " windowFocused=" + windowFocused);
                for (Node n : root.lookupAll(".spec-live-focus")) {
                    n.requestFocus();
                    n.pseudoClassStateChanged(Boards.FOCUSED, true);
                }
                root.applyCss();
                root.layout();
                // Wrapped labels (dialog body) settle their height only after their final width is known:
                // invalidate dialog panes and lay out again, as successive pulses would on a shown dialog.
                for (Node n : root.lookupAll(".dialog-pane")) if (n instanceof Parent p) p.requestLayout();
                root.applyCss();
                root.layout();
                root.layout();
                SnapshotParameters params = new SnapshotParameters();
                params.setTransform(Transform.scale(SCALE, SCALE));
                WritableImage full = root.snapshot(params, null);
                writePng(full, output.resolve(shot.name + "-full.png"), 0, 0, (int) full.getWidth(), (int) full.getHeight());
                // Detail crop: the top-left 640x410 logical region, at native 2x.
                writePng(full, output.resolve(shot.name + "-fold.png"), 0, 0, (int) (W * SCALE / 2), (int) (H * SCALE / 2));
                written.add(shot.name);
            });
        }

        private void next() {
            Step step = steps.poll();
            if (step == null) return;
            PauseTransition pause = new PauseTransition(Duration.millis(180));
            pause.setOnFinished(e -> {
                try { step.run(); next(); } catch (Throwable error) { fail(error); }
            });
            pause.play();
        }

        private void finish() throws IOException {
            List<String> lines = new ArrayList<>();
            for (String key : List.of("java.version", "javafx.runtime.version", "os.name", "os.version", "os.arch"))
                lines.add(key + "=" + System.getProperty(key, "unknown"));
            lines.add("outputScale=" + stage.getOutputScaleX());
            lines.add("shots=" + String.join(",", written));
            lines.add("focus=" + String.join("; ", focusLog));
            Files.write(output.resolve("runtime.properties"), lines);
            System.out.println("MERIDIAN_GALLERY_OK shots=" + written.size());
            binding.close();
            stage.close();
            Platform.exit();
        }

        private void fail(Throwable error) {
            failure = error;
            error.printStackTrace();
            for (Window w : new ArrayList<>(Window.getWindows())) if (w instanceof PopupWindow p) p.hide();
            if (binding != null) binding.close();
            stage.close();
            Platform.exit();
        }
    }

    @FunctionalInterface interface Step { void run() throws Exception; }

    /** Board builders. Content is frozen across rounds. */
    static final class Boards {
        static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");
        static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
        static final PseudoClass FOCUS_VISIBLE = PseudoClass.getPseudoClass("focus-visible");
        static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");
        static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("success");

        static void forceStates(Parent root) {
            root.applyCss();
            for (Node n : root.lookupAll(".spec-hover")) n.pseudoClassStateChanged(HOVER, true);
            for (Node n : root.lookupAll(".spec-pressed")) if (n instanceof Button b) b.arm();
            for (Node n : root.lookupAll(".spec-focus")) {
                n.pseudoClassStateChanged(FOCUSED, true);
                n.pseudoClassStateChanged(FOCUS_VISIBLE, true);
            }
            for (Node n : root.lookupAll(".spec-live-focus")) n.requestFocus();
        }

        static Parent header(String title, String mode) {
            Label t = label(title, "meridian-title");
            Label m = label("Meridian · " + mode, "meridian-secondary");
            Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
            HBox h = new HBox(t, gap, m); h.setAlignment(Pos.BASELINE_LEFT);
            return h;
        }

        static VBox section(String title, Node... content) {
            VBox v = new VBox(10); v.getChildren().add(label(title, "meridian-section")); v.getChildren().addAll(content);
            return v;
        }

        static Button btn(String text, String... classes) { Button b = new Button(text); b.getStyleClass().addAll(classes); return b; }

        static Parent controls(String mode) {
            // Buttons
            HBox variants = row(btn("Default"), btn("Accent", Styles.ACCENT), btn("Success", Styles.SUCCESS),
                    btn("Danger", Styles.DANGER), btn("Outlined", Styles.BUTTON_OUTLINED), btn("Flat", Styles.FLAT),
                    btn("Small", Styles.SMALL), btn("Large", Styles.LARGE));
            Button dRest = btn("Rest"), dHover = btn("Hover", "spec-hover"), dPressed = btn("Pressed", "spec-pressed"),
                    dFocus = btn("Focus", "spec-focus"), dDisabled = btn("Disabled");
            dDisabled.setDisable(true);
            Button aRest = btn("Rest", Styles.ACCENT), aHover = btn("Hover", Styles.ACCENT, "spec-hover"),
                    aPressed = btn("Pressed", Styles.ACCENT, "spec-pressed"), aFocus = btn("Focus", Styles.ACCENT, "spec-focus"),
                    aDisabled = btn("Disabled", Styles.ACCENT);
            aDisabled.setDisable(true);
            GridPane states = new GridPane(); states.setHgap(10); states.setVgap(10);
            states.addRow(0, label("Default", "meridian-metadata", "meridian-secondary"), dRest, dHover, dPressed, dFocus, dDisabled);
            states.addRow(1, label("Accent", "meridian-metadata", "meridian-secondary"), aRest, aHover, aPressed, aFocus, aDisabled);

            // Inputs
            TextField value = new TextField("Recall correspondence");
            TextField prompt = new TextField(); prompt.setPromptText("Search datasets…");
            TextField invalid = new TextField("-4"); invalid.pseudoClassStateChanged(DANGER, true);
            TextField focusField = new TextField("Focused field"); focusField.getStyleClass().add("spec-focus");
            TextField disabledField = new TextField("Disabled"); disabledField.setDisable(true);
            PasswordField password = new PasswordField(); password.setText("secret");
            ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("Gaussian", "Boxcar", "Hemodynamic"));
            combo.getSelectionModel().select(0);
            ChoiceBox<String> choice = new ChoiceBox<>(FXCollections.observableArrayList("Session 1", "Session 2"));
            choice.getSelectionModel().select(1);
            Spinner<Integer> spinner = new Spinner<>(0, 100, 12); spinner.setEditable(true);
            DatePicker date = new DatePicker(LocalDate.of(2026, 10, 15));
            MenuButton menuButton = new MenuButton("Export", null, new MenuItem("CSV"), new MenuItem("Parquet"));
            SplitMenuButton split = new SplitMenuButton(new MenuItem("Run all"), new MenuItem("Run selected"));
            split.setText("Run");
            for (Region r : List.of(value, prompt, invalid, focusField, disabledField, password, combo, choice, spinner, date))
                r.setMaxWidth(Double.MAX_VALUE);
            GridPane inputs = new GridPane(); inputs.setHgap(12); inputs.setVgap(10);
            inputs.addRow(0, fieldLabel("Name"), value, fieldLabel("Kernel"), combo);
            inputs.addRow(1, fieldLabel("Search"), prompt, fieldLabel("Session"), choice);
            inputs.addRow(2, fieldLabel("Window"), invalid, fieldLabel("Repeats"), spinner);
            inputs.addRow(3, fieldLabel("Focus"), focusField, fieldLabel("Date"), date);
            inputs.addRow(4, fieldLabel("Disabled"), disabledField, fieldLabel("Password"), password);
            Label error = label("Enter a positive window size.", Styles.DANGER, "meridian-metadata");
            inputs.add(error, 1, 5);
            inputs.add(row(menuButton, split), 3, 5);
            for (int c : new int[] {1, 3}) {
                var cc = new javafx.scene.layout.ColumnConstraints(); cc.setHgrow(Priority.ALWAYS);
                cc.setPrefWidth(200);
                while (inputs.getColumnConstraints().size() < c) inputs.getColumnConstraints().add(new javafx.scene.layout.ColumnConstraints());
                inputs.getColumnConstraints().add(cc);
            }

            // Selection
            CheckBox c1 = new CheckBox("Unchecked"), c2 = new CheckBox("Checked"), c3 = new CheckBox("Mixed"), c4 = new CheckBox("Disabled");
            c2.setSelected(true); c3.setAllowIndeterminate(true); c3.setIndeterminate(true); c4.setDisable(true); c4.setSelected(true);
            ToggleGroup rg = new ToggleGroup();
            RadioButton r1 = new RadioButton("Left"), r2 = new RadioButton("Right"), r3 = new RadioButton("Disabled");
            r1.setToggleGroup(rg); r2.setToggleGroup(rg); r3.setToggleGroup(rg); r1.setSelected(true); r3.setDisable(true);
            ToggleSwitch s1 = new ToggleSwitch("Autosave"), s2 = new ToggleSwitch("Sync");
            s1.setSelected(true);
            ToggleGroup tg = new ToggleGroup();
            ToggleButton t1 = new ToggleButton("Day"), t2 = new ToggleButton("Week"), t3 = new ToggleButton("Month");
            t1.getStyleClass().add(Styles.LEFT_PILL); t2.getStyleClass().add(Styles.CENTER_PILL); t3.getStyleClass().add(Styles.RIGHT_PILL);
            for (ToggleButton t : List.of(t1, t2, t3)) t.setToggleGroup(tg);
            t2.setSelected(true);
            SegmentedControl segmented = new SegmentedControl("Raw", "Filtered", "Model");
            VBox selection = new VBox(10, row(c1, c2, c3, c4), row(r1, r2, r3), row(s1, s2), row(new HBox(t1, t2, t3), segmented));

            // Range & progress
            Slider slider = new Slider(0, 100, 40); slider.setPrefWidth(220);
            ProgressBar bar = new ProgressBar(0.62); bar.setPrefWidth(220);
            ProgressIndicator ring = new ProgressIndicator(0.62);
            VBox range = new VBox(12, row(label("Threshold"), slider), row(label("Upload"), bar, ring), row(new Hyperlink("View documentation"), new Hyperlink("Disabled link") {{ setDisable(true); }}));

            // Type & messages
            VBox type = new VBox(4, label("Title — Episodic recall", "meridian-title"), label("Section — Trial summary", "meridian-section"),
                    label("Body — The quick brown fox jumps over the lazy dog. 0123456789"),
                    label("Secondary — Supporting description text", "meridian-secondary"),
                    label("Metadata — Updated 3 minutes ago · 24 runs", "meridian-metadata", "meridian-secondary"));
            Message info = new Message("Preprocessing complete", "24 runs ready for model fitting.");
            info.getStyleClass().add(Styles.ACCENT);
            Message warn = new Message("Motion outliers", "3 runs exceed the framewise threshold.");
            warn.getStyleClass().add(Styles.WARNING);
            Message bad = new Message("Fit failed", "Design matrix is rank deficient.");
            bad.getStyleClass().add(Styles.DANGER);
            VBox messages = new VBox(8, info, warn, bad);

            TabPane tabs = new TabPane(new Tab("Overview", new Label("  ")), new Tab("Parameters"), new Tab("Logs"));
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); tabs.setPrefHeight(40); tabs.setMaxHeight(40);
            TextArea area = new TextArea("Notes about this analysis.\nSecond line."); area.setPrefRowCount(3);

            VBox left = new VBox(22, section("Buttons", variants, states), section("Inputs", inputs), section("Tabs & text area", tabs, area));
            VBox right = new VBox(22, section("Selection", selection), section("Range & progress", range), section("Type", type), section("Messages", messages));
            left.setPrefWidth(700); right.setPrefWidth(480);
            HBox body = new HBox(40, left, new Separator(javafx.geometry.Orientation.VERTICAL), right);
            VBox root = new VBox(20, header("Controls", mode), body);
            root.setPadding(new Insets(28));
            return root;
        }

        static Parent data(String mode) {
            TreeView<String> tree = studyTree();
            tree.getSelectionModel().select(3);
            TableView<Trial> table = trials(40);
            table.getSelectionModel().select(2);
            table.getStyleClass().add("spec-live-focus");
            TableColumn<Trial, ?> sorted = table.getColumns().get(3);
            sorted.setSortType(TableColumn.SortType.DESCENDING);
            table.getSortOrder().add(sorted);
            ListView<String> list = new ListView<>(FXCollections.observableArrayList(
                    "Gaussian kernel", "Boxcar kernel", "Hemodynamic response", "Finite impulse", "Gamma basis", "Spline basis", "Polynomial drift"));
            list.getSelectionModel().select(2);
            list.setPrefHeight(230);
            TitledPane p1 = new TitledPane("Acquisition", new VBox(6, new Label("TR 1.5 s"), new Label("Slices 72")));
            TitledPane p2 = new TitledPane("Preprocessing", new Label("fMRIPrep 25.1"));
            TitledPane p3 = new TitledPane("Model", new Label("GLM, AR(1)"));
            Accordion accordion = new Accordion(p1, p2, p3); accordion.setExpandedPane(p1);
            VBox right = new VBox(16, section("Basis functions", list), section("Details", accordion));
            right.setPrefWidth(300);
            VBox treeBox = section("Study", tree); VBox.setVgrow(tree, Priority.ALWAYS); treeBox.setPrefWidth(250);
            VBox tableBox = section("Trials", table); VBox.setVgrow(table, Priority.ALWAYS);
            HBox body = new HBox(20, treeBox, tableBox, right); HBox.setHgrow(tableBox, Priority.ALWAYS);
            VBox.setVgrow(body, Priority.ALWAYS);
            VBox root = new VBox(20, header("Data views", mode), body);
            root.setPadding(new Insets(28));
            return root;
        }

        static Parent app(String mode) {
            Menu file = new Menu("File"), edit = new Menu("Edit"), view = new Menu("View"), analysis = new Menu("Analysis"), help = new Menu("Help");
            MenuBar menuBar = new MenuBar(file, edit, view, analysis, help);
            TextField search = new TextField(); search.setPromptText("Search  ⌘K"); search.setPrefWidth(240);
            ComboBox<String> model = new ComboBox<>(FXCollections.observableArrayList("GLM · AR(1)", "LSS", "MVPA"));
            model.getSelectionModel().select(0);
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            ToolBar toolbar = new ToolBar(btn("Open", Styles.FLAT), btn("Save", Styles.FLAT), new Separator(javafx.geometry.Orientation.VERTICAL),
                    model, btn("Run model", Styles.ACCENT), spacer, search);

            Label navTitle = label("WORKSPACE", "meridian-metadata", "meridian-secondary");
            TreeView<String> tree = studyTree();
            tree.getSelectionModel().select(4);
            VBox nav = new VBox(8, navTitle, tree); VBox.setVgrow(tree, Priority.ALWAYS);
            nav.setPadding(new Insets(12)); nav.getStyleClass().add("meridian-shell");

            TableView<Trial> table = trials(60);
            table.getSelectionModel().select(5);
            table.getStyleClass().add("spec-live-focus");
            TabPane tabs = new TabPane(new Tab("Trials", table), new Tab("Contrasts"), new Tab("Diagnostics"));
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            VBox center = new VBox(12, row(label("sub-07 · Episodic recall", "meridian-section"), statusPill("Fitted")), tabs);
            VBox.setVgrow(tabs, Priority.ALWAYS); center.setPadding(new Insets(16));

            GridPane form = new GridPane(); form.setHgap(10); form.setVgap(10);
            TextField name = new TextField("recall_vs_baseline");
            ComboBox<String> hrf = new ComboBox<>(FXCollections.observableArrayList("Canonical", "Derivatives"));
            hrf.getSelectionModel().select(0);
            Spinner<Double> fwhm = new Spinner<>(0.0, 12.0, 6.0, 0.5); fwhm.setEditable(true);
            TextField threshold = new TextField("0.001");
            for (Region r : List.of(name, hrf, fwhm, threshold)) r.setMaxWidth(Double.MAX_VALUE);
            form.addRow(0, fieldLabel("Contrast"), name);
            form.addRow(1, fieldLabel("HRF"), hrf);
            form.addRow(2, fieldLabel("Smoothing"), fwhm);
            form.addRow(3, fieldLabel("p threshold"), threshold);
            var c0 = new javafx.scene.layout.ColumnConstraints(); var c1 = new javafx.scene.layout.ColumnConstraints();
            c1.setHgrow(Priority.ALWAYS); form.getColumnConstraints().addAll(c0, c1);
            ToggleSwitch fdr = new ToggleSwitch("FDR correction"); fdr.setSelected(true);
            CheckBox cache = new CheckBox("Cache residuals");
            Region grow = new Region(); VBox.setVgrow(grow, Priority.ALWAYS);
            Button cancel = btn("Reset"), apply = btn("Apply", Styles.ACCENT);
            HBox actions = new HBox(8, cancel, apply); actions.setAlignment(Pos.CENTER_RIGHT);
            VBox inspector = new VBox(14, label("Inspector", "meridian-section"), form, fdr, cache, new Separator(),
                    label("Estimated runtime 2m 14s", "meridian-metadata", "meridian-secondary"), grow, actions);
            inspector.setPadding(new Insets(16)); inspector.setPrefWidth(300);

            SplitPane split = new SplitPane(nav, center, inspector);
            split.setDividerPositions(0.2, 0.76);
            ProgressBar progress = new ProgressBar(0.4); progress.setPrefWidth(120);
            Region sgap = new Region(); HBox.setHgrow(sgap, Priority.ALWAYS);
            HBox status = new HBox(12, label("Ready", "meridian-metadata"), label("60 trials · 4 conditions", "meridian-metadata", "meridian-secondary"),
                    sgap, label("Fitting sub-08", "meridian-metadata", "meridian-secondary"), progress);
            status.setAlignment(Pos.CENTER_LEFT); status.setPadding(new Insets(4, 12, 4, 12)); status.getStyleClass().add("meridian-shell");
            BorderPane root = new BorderPane(split, new VBox(menuBar, toolbar), null, status, null);
            root.setUserData(mode);
            return root;
        }

        static Label statusPill(String text) {
            Label l = label(text, Styles.SUCCESS, "meridian-metadata");
            return l;
        }

        static TreeView<String> studyTree() {
            TreeItem<String> rootItem = new TreeItem<>("Episodic recall study");
            for (int s = 5; s <= 9; s++) {
                TreeItem<String> sub = new TreeItem<>("sub-0" + s);
                for (int r = 1; r <= 2; r++) sub.getChildren().add(new TreeItem<>("run-0" + r + " · bold"));
                sub.setExpanded(s <= 7);
                rootItem.getChildren().add(sub);
            }
            rootItem.setExpanded(true);
            TreeView<String> tree = new TreeView<>(rootItem);
            tree.setPrefHeight(500);
            return tree;
        }

        record Trial(int id, String condition, String onset, double rt, String accuracy, String status) {}

        static TableView<Trial> trials(int n) {
            String[] conditions = {"Recall", "Familiar", "Novel", "Baseline"};
            String[] statuses = {"Included", "Included", "Included", "Flagged"};
            var items = FXCollections.<Trial>observableArrayList();
            for (int i = 1; i <= n; i++) {
                items.add(new Trial(i, conditions[i % 4], String.format("%.2f s", i * 7.5),
                        412 + ((i * 37) % 290), (i % 7 == 0) ? "0" : "1", statuses[(i * 3) % 4]));
            }
            TableView<Trial> table = new TableView<>(items);
            table.getColumns().add(col("Trial", 70, t -> t.id()));
            table.getColumns().add(col("Condition", 130, t -> t.condition()));
            table.getColumns().add(col("Onset", 110, t -> t.onset()));
            table.getColumns().add(col("RT (ms)", 100, t -> t.rt()));
            table.getColumns().add(col("Correct", 90, t -> t.accuracy()));
            table.getColumns().add(col("Status", 120, t -> t.status()));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            return table;
        }

        static <T> TableColumn<Trial, T> col(String title, double width, java.util.function.Function<Trial, T> f) {
            TableColumn<Trial, T> c = new TableColumn<>(title);
            c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(f.apply(d.getValue())));
            c.setPrefWidth(width);
            return c;
        }
    }

    /** Overlay board: popups are opened for real, captured, and placed as images beside an inline dialog pane. */
    static final class Overlays {
        final VBox root;
        final FlowPane gallery = new FlowPane(28, 28);
        final Button anchor = Boards.btn("Anchor");
        final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(
                "Gaussian", "Boxcar", "Hemodynamic", "Finite impulse", "Gamma basis"));
        final DatePicker date = new DatePicker(LocalDate.of(2026, 10, 15));
        final MenuBar menuBar;
        final Menu analysis;
        final List<Node> captured = new ArrayList<>();
        final List<String> captions = new ArrayList<>();
        String pending;
        Runnable closer;

        Overlays(String mode) {
            combo.getSelectionModel().select(2);
            combo.setPrefWidth(200);
            analysis = new Menu("Analysis");
            Menu recent = new Menu("Recent models", null, new MenuItem("GLM · AR(1)"), new MenuItem("LSS"));
            MenuItem run = new MenuItem("Run model"); run.setAccelerator(KeyCombination.keyCombination("Shortcut+R"));
            MenuItem stop = new MenuItem("Stop"); stop.setDisable(true);
            CheckMenuItem live = new CheckMenuItem("Live preview"); live.setSelected(true);
            analysis.getItems().addAll(run, stop, new SeparatorMenuItem(), live, recent);
            menuBar = new MenuBar(new Menu("File"), new Menu("Edit"), analysis);

            DialogPane dialog = new DialogPane();
            dialog.setHeaderText("Delete 3 runs?");
            dialog.setContentText("The selected runs and their derived outputs will be removed. This cannot be undone.");
            dialog.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dialog.setPrefWidth(420);
            ((Button) dialog.lookupButton(ButtonType.OK)).setText("Delete");
            Notification toast = new Notification("Model fitted for sub-07 in 2m 14s.");
            toast.setPrefWidth(360); toast.setMaxHeight(Region.USE_PREF_SIZE);
            VBox inline = new VBox(24, Boards.section("Dialog", dialog), Boards.section("Notification", toast));
            HBox anchors = new HBox(16, anchor, combo, date, menuBar);
            anchors.setAlignment(Pos.CENTER_LEFT);
            gallery.setPrefWrapLength(720);
            VBox popups = Boards.section("Popups (captured live)", gallery);
            HBox body = new HBox(40, inline, popups);
            root = new VBox(20, Boards.header("Overlays", mode), anchors, body);
            root.setPadding(new Insets(28));
        }

        void openContext(Stage owner) {
            ContextMenu menu = new ContextMenu();
            MenuItem inspect = new MenuItem("Inspect dataset"); inspect.setAccelerator(KeyCombination.keyCombination("Shortcut+I"));
            MenuItem export = new MenuItem("Export summary");
            Menu sendTo = new Menu("Send to", null, new MenuItem("Notebook"), new MenuItem("Report"));
            MenuItem unavailable = new MenuItem("Unavailable"); unavailable.setDisable(true);
            MenuItem delete = new MenuItem("Delete"); delete.getStyleClass().add(Styles.DANGER);
            menu.getItems().addAll(inspect, export, sendTo, new SeparatorMenuItem(), unavailable, delete);
            menu.show(anchor, Side.BOTTOM, 0, 6);
            Platform.runLater(() -> {
                Node first = inspect.getStyleableNode();
                if (first != null) first.pseudoClassStateChanged(Boards.FOCUSED, true);
            });
            pending = "Context menu (first item focused)";
            closer = menu::hide;
        }

        void openCombo() { combo.show(); pending = "Combo box list"; closer = combo::hide; }
        void openDate() { date.show(); pending = "Date picker"; closer = date::hide; }
        void openMenuBar() { analysis.show(); pending = "Menu bar menu"; closer = analysis::hide; }
        void openTooltip(Stage owner) {
            Tooltip tip = new Tooltip("Runs the selected model on all included trials");
            javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            tip.show(owner, b.getMinX(), b.getMaxY() + 8);
            pending = "Tooltip";
            closer = tip::hide;
        }

        void capture() throws Exception {
            List<PopupWindow> showing = new ArrayList<>();
            for (Window w : Window.getWindows()) if (w instanceof PopupWindow p && p.isShowing()) showing.add(p);
            if (showing.isEmpty()) throw new IllegalStateException("No popup showing for " + pending);
            // Capture every showing popup (a submenu-free menu yields one); combine side by side.
            HBox combined = new HBox(8);
            for (PopupWindow p : showing) {
                Parent r = p.getScene().getRoot();
                r.applyCss(); r.layout();
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                params.setTransform(Transform.scale(SCALE, SCALE));
                WritableImage img = r.snapshot(params, null);
                ImageView view = new ImageView(img);
                view.setFitWidth(img.getWidth() / SCALE); view.setFitHeight(img.getHeight() / SCALE);
                view.setSmooth(true);
                combined.getChildren().add(view);
            }
            closer.run();
            VBox cell = new VBox(6, label(pending, "meridian-metadata", "meridian-secondary"), combined);
            captured.add(cell);
        }

        void place() { gallery.getChildren().setAll(captured); }
    }

    static HBox row(Node... nodes) { HBox h = new HBox(10, nodes); h.setAlignment(Pos.CENTER_LEFT); return h; }
    static Label fieldLabel(String text) { return label(text, "meridian-secondary"); }
    static Label label(String text, String... classes) { Label x = new Label(text); x.getStyleClass().addAll(classes); return x; }

    static void writePng(WritableImage image, Path output, int x0, int y0, int w, int h) throws IOException {
        w = Math.min(w, (int) image.getWidth() - x0); h = Math.min(h, (int) image.getHeight() - y0);
        BufferedImage png = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) png.setRGB(x, y, reader.getArgb(x0 + x, y0 + y));
        if (!ImageIO.write(png, "png", output.toFile())) throw new IOException("No PNG writer");
    }
}
