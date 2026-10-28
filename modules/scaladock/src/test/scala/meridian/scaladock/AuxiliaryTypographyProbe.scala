package meridian.scaladock

import java.nio.file.{Files, Path}
import javafx.animation.PauseTransition
import javafx.application.{Application, Platform}
import javafx.scene.{Node as FxNode, Scene}
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
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

object AuxiliaryTypographyProbe:
  @volatile private[scaladock] var failure: Option[Throwable] = None
  final case class Content(name: String) derives ReadWriter
  val Pane = PaneType[Content]("meridian.auxiliary-type")
  def main(args: Array[String]): Unit =
    Application.launch(classOf[AuxiliaryTypographyApp], args*)
    failure.foreach(error => throw IllegalStateException("Auxiliary typography failed", error))

final class AuxiliaryTypographyApp extends Application:
  import AuxiliaryTypographyProbe.*
  private var dock: Option[Dock]                                    = None
  private var appearance: Option[MeridianAppearance]                = None
  private var binding: Option[MeridianDock]                         = None
  private var registration: Option[MeridianAppearance#Registration] = None
  private var window: Option[Stage]                                 = None
  private var destination                                           = Path.of(".")
  private var phase                                                 = 0
  private val reports  = scala.collection.mutable.ArrayBuffer.empty[ujson.Value]
  private val swatches = scala.collection.mutable.ArrayBuffer.empty[Rectangle]
  private val cases =
    for
      theme   <- Vector(Appearance.LIGHT, Appearance.DARK)
      density <- Vector(DensityMode.COMPACT, DensityMode.COMFORTABLE)
      scale   <- Vector(1.0, 1.5, 2.0)
    yield (theme, density, scale)
  private def value: Dock             = dock.get
  private def app: MeridianAppearance = appearance.get
  private def visible(node: FxNode): Boolean =
    node.isVisible && (node.getParent == null || visible(node.getParent))
  private def later(body: => Unit): Unit =
    val pause = new PauseTransition(Duration.millis(150))
    pause.setOnFinished: _ =>
      try body
      catch case error: Throwable => fail(error)
    pause.play()

  override def start(stage: Stage): Unit =
    window = Some(stage)
    try
      require(getParameters.getRaw.size == 1, "Pass a new evidence directory")
      destination = Path.of(getParameters.getRaw.get(0))
      Files.createDirectory(destination)
      val factories = PaneFactories.empty.register(Pane): _ =>
        val swatch = new Rectangle(30, 30, Color.MAGENTA)
        swatches += swatch
        new PaneView[Content]:
          val node: FxNode        = new StackPane(swatch)
          def snapshot(): Content = Content("retained")
      def pane(name: String): PaneDef = Pane(Content(name)).titled(name)
      dock = Some(Dock(
        factories,
        LayoutState.of(column(
          group(
            pane("Long analysis definition one"),
            pane("Long analysis definition two"),
            pane("Long analysis definition three")
          ),
          group(pane("Record"))
        ))
      ))
      appearance = Some(MeridianAppearance.install())
      val scene = new Scene(value.view, 350, 300)
      registration = Some(app.register(scene))
      binding = Some(MeridianDock.bind(value, app))
      stage.setScene(scene)
      stage.show()
      prepare()
    catch case error: Throwable => fail(error)

  private def prepare(): Unit =
    val (theme, density, scale) = cases(phase)
    val before                  = value.state
    app.setPreferences(new MeridianPreferences(theme, density, scale, Motion.REDUCED))
    require(value.state == before, "Appearance changed layout identity")
    later:
      val expected = 12 * scale
      val labels = value.view.lookupAll(".dock-tab-overflow > .label").asScala.collect:
        case label: Label if visible(label) && label.getText.nonEmpty => label
      require(labels.nonEmpty, "No actual overflow counter was exercised")
      labels.foreach(label =>
        require(
          math.abs(label.getFont.getSize - expected) < 1e-6,
          s"Overflow counter font ${label.getFont.getSize}, expected $expected"
        )
      )
      value.focus(value.state.panes.find(_.title == "Long analysis definition one").get.id)
      value.toggleMaximizeFocused()
      later:
        val badges = value.view.lookupAll(".dock-maximized-badge").asScala.collect:
          case label: Label if visible(label) && label.getText.nonEmpty => label
        require(badges.nonEmpty, "No actual hidden-group badge was exercised")
        badges.foreach(label =>
          require(
            math.abs(label.getFont.getSize - expected) < 1e-6,
            s"Maximized counter font ${label.getFont.getSize}, expected $expected"
          )
        )
        require(
          swatches.forall(_.getFill == Color.MAGENTA),
          "Appearance changed scientific swatches"
        )
        reports += ujson.Obj(
          "theme"                -> theme.toString,
          "density"              -> density.toString,
          "text_scale"           -> scale,
          "expected_metadata_px" -> expected,
          "overflow_counters"    -> labels.size,
          "maximized_badges"     -> badges.size,
          "scientific_swatches"  -> "unchanged"
        )
        value.restore()
        phase += 1
        if phase < cases.size then prepare()
        else
          Files.writeString(
            destination.resolve("auxiliary-typography.json"),
            ujson.write(
              ujson.Obj(
                "schema" -> "meridian.auxiliary-typography.v1",
                "javafx" -> System.getProperty("javafx.runtime.version"),
                "status" -> "passed",
                "cases"  -> ujson.Arr.from(reports)
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
    AuxiliaryTypographyProbe.failure = Some(error)
    error.printStackTrace()
    cleanup()
