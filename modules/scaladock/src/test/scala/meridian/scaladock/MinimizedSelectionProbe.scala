package meridian.scaladock

import java.nio.file.{Files, Path}
import javafx.animation.PauseTransition
import javafx.application.{Application, Platform}
import javafx.css.PseudoClass
import javafx.event.Event
import javafx.scene.{Node as FxNode, Scene}
import javafx.scene.control.Button
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.{BorderPane, Region, StackPane}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.Duration
import meridian.theme.{MeridianAppearance, MeridianPreferences}
import meridian.theme.MeridianPreferences.{Appearance, Motion}
import meridian.theme.metrics.MeridianMetrics.DensityMode
import _root_.scaladock.*
import _root_.scaladock.dsl.*
import _root_.scaladock.fx.*
import scala.jdk.CollectionConverters.*
import upickle.default.ReadWriter

object MinimizedSelectionProbe:
  @volatile private[scaladock] var failure: Option[Throwable] = None
  final case class Content(name: String) derives ReadWriter
  val Pane = PaneType[Content]("meridian.minimized-selection")
  def main(args: Array[String]): Unit =
    Application.launch(classOf[MinimizedSelectionApp], args*)
    failure.foreach(error => throw IllegalStateException("Minimized selection failed", error))

final class MinimizedSelectionApp extends Application:
  import MinimizedSelectionProbe.*
  private var dock: Option[Dock]                                    = None
  private var appearance: Option[MeridianAppearance]                = None
  private var binding: Option[MeridianDock]                         = None
  private var registration: Option[MeridianAppearance#Registration] = None
  private var window: Option[Stage]                                 = None
  private var destination                                           = Path.of(".")
  private var phase                                                 = 0
  private val global                                                = new Button("Global action")
  private val roots    = scala.collection.mutable.Map.empty[String, StackPane]
  private val swatches = scala.collection.mutable.ArrayBuffer.empty[Rectangle]
  private val reports  = scala.collection.mutable.ArrayBuffer.empty[ujson.Value]
  private val cases =
    for
      theme   <- Vector(Appearance.LIGHT, Appearance.DARK)
      density <- Vector(DensityMode.COMPACT, DensityMode.COMFORTABLE)
      scale   <- Vector(1.0, 1.5, 2.0)
    yield (theme, density, scale)
  private def value = dock.get
  private def app   = appearance.get
  private def later(body: => Unit): Unit =
    val pause = new PauseTransition(Duration.millis(150))
    pause.setOnFinished(_ =>
      try body
      catch case error: Throwable => fail(error)
    )
    pause.play()
  private def fills(node: FxNode): Vector[Color] =
    Option(
      node.asInstanceOf[Region].getBackground
    ).toVector.flatMap(_.getFills.asScala.map(_.getFill.asInstanceOf[Color]))
  private def id(name: String) = value.state.panes.find(_.title == name).get.id
  private def selected: FxNode =
    value.view.lookupAll(".dock-tab").asScala.find(_.getAccessibleText == "Data").get
  private def owningGroup(node: FxNode): FxNode =
    if node.getStyleClass.contains("dock-group") then node else owningGroup(node.getParent)
  private lazy val palettes =
    val stream = getClass.getResourceAsStream("/meridian/theme/tokens.json")
    require(stream != null, "Missing provider tokens")
    try ujson.read(new String(
        stream.readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
      ))("appearances")
    finally stream.close()

  override def start(stage: Stage): Unit =
    window = Some(stage)
    try
      require(getParameters.getRaw.size == 1, "Pass a new evidence directory")
      destination = Path.of(getParameters.getRaw.get(0))
      Files.createDirectory(destination)
      val factories = PaneFactories.empty.register(Pane): state =>
        val swatch = new Rectangle(30, 30, Color.MAGENTA)
        swatches += swatch
        val root = new StackPane(swatch)
        root.setFocusTraversable(true)
        roots(state.name) = root
        new PaneView[Content]:
          val node: FxNode        = root
          def snapshot(): Content = state
      dock = Some(Dock(
        factories,
        LayoutState.of(column(
          group(Pane(Content("Data")).titled("Data")),
          group(Pane(Content("Inspector")).titled("Inspector"))
        ))
      ))
      appearance = Some(MeridianAppearance.install())
      val scene = new Scene(new BorderPane(value.view, global, null, null, null), 700, 500)
      registration = Some(app.register(scene))
      binding = Some(MeridianDock.bind(value, app))
      stage.setScene(scene)
      stage.show()
      prepare()
    catch case error: Throwable => fail(error)

  private def prepare(): Unit =
    val (theme, density, scale) = cases(phase)
    app.setPreferences(new MeridianPreferences(theme, density, scale, Motion.REDUCED))
    value.focus(id("Data"))
    global.requestFocus()
    later:
      val normal = fills(selected)
      require(normal.exists(_.getOpacity > 0), "Normal selected-pane cue disappeared")
      val before = value.state.root
      val header = value.settings.headerPx
      value.toggleMinimizeFocused()
      later:
        val samples =
          for
            inactiveWindow <- Vector(false, true)
            state          <- Vector("active-global-focus", "active-tab-focus", "inactive-group")
          yield
            if state == "inactive-group" then
              value.focus(id("Inspector"))
              roots("Inspector").requestFocus()
            else
              // Keep the minimized group active without expanding it via edit.focus.
              value.view.lookupAll(
                ".dock-tab"
              ).asScala.find(_.getAccessibleText == "Data").get.requestFocus()
              if state == "active-global-focus" then global.requestFocus()
            value.view.pseudoClassStateChanged(
              PseudoClass.getPseudoClass("inactive"),
              inactiveWindow
            )
            value.view.applyCss()
            val paints  = fills(selected)
            val focused = selected.isFocused
            val row = ujson.Obj(
              "state"              -> state,
              "inactive_css_state" -> inactiveWindow,
              "focused"            -> focused,
              "active_group" -> owningGroup(
                selected
              ).getPseudoClassStates.contains(PseudoClass.getPseudoClass("active")),
              "paints"    -> ujson.Arr.from(paints.map(c => ujson.Str(c.toString))),
              "header_px" -> value.settings.headerPx
            )
            Files.writeString(
              destination.resolve(
                s"case-$phase-${if inactiveWindow then "inactive" else "active"}-$state.json"
              ),
              ujson.write(row, indent = 2) + "\n"
            )
            require(
              value.state.minimized(value.state.groupOf(id("Data")).get.id),
              "Focus expanded minimized content"
            )
            require(focused == (state == "active-tab-focus"), s"Wrong actual focus for $state")
            if focused then
              val ring =
                palettes(if theme == Appearance.DARK then "dark" else "light")("focus.ring").str
              require(paints.contains(Color.web(ring)), "Minimized keyboard focus ring missing")
              require(paints.size == 2, s"Focus has selected-pane layers: $paints")
            else
              require(
                paints.forall(_.getOpacity == 0),
                s"Minimized tab painted selected pane: $paints"
              )
            require(value.settings.headerPx == header, "Focus changed header geometry")
            row
        value.view.pseudoClassStateChanged(PseudoClass.getPseudoClass("inactive"), false)
        selected.requestFocus()
        Event.fireEvent(
          selected,
          new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false)
        )
        require(
          value.state.minimized.isEmpty && value.state.root == before,
          "Restore changed layout"
        )
        require(swatches.forall(_.getFill == Color.MAGENTA), "Scientific colours changed")
        reports += ujson.Obj(
          "theme"                  -> theme.toString,
          "density"                -> density.toString,
          "text_scale"             -> scale,
          "normal_selected_paints" -> ujson.Arr.from(normal.map(c => ujson.Str(c.toString))),
          "samples"                -> ujson.Arr.from(samples)
        )
        phase += 1
        if phase < cases.size then prepare()
        else
          Files.writeString(
            destination.resolve("minimized-selection.json"),
            ujson.write(
              ujson.Obj(
                "schema"                -> "meridian.minimized-selection.v1",
                "status"                -> "passed",
                "javafx"                -> System.getProperty("javafx.runtime.version"),
                "inactive_window_scope" -> "CSS state simulation; not OS window automation",
                "cases"                 -> ujson.Arr.from(reports)
              ),
              indent = 2
            ) + "\n"
          )
          cleanup()

  private def cleanup(): Unit =
    binding.foreach(_.close())
    registration.foreach(_.close())
    dock.foreach(_.dispose())
    appearance.foreach(_.close())
    window.foreach(_.close())
    Platform.exit()
  private def fail(error: Throwable): Unit =
    MinimizedSelectionProbe.failure = Some(error)
    error.printStackTrace()
    cleanup()
