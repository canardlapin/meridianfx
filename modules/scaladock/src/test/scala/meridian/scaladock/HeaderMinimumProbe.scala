package meridian.scaladock

import java.nio.file.{Files, Path}
import javafx.animation.PauseTransition
import javafx.application.{Application, Platform}
import javafx.scene.{Node as FxNode, Parent, Scene}
import javafx.scene.control.Label
import javafx.scene.layout.{Region, StackPane}
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

/** App geometry must survive live preferences without overriding provider text floors. */
object HeaderMinimumProbe:
  @volatile private[scaladock] var failure: Option[Throwable] = None
  final case class Content(name: String) derives ReadWriter
  val Pane = PaneType[Content]("meridian.header-minimum")
  def main(args: Array[String]): Unit =
    Application.launch(classOf[HeaderMinimumApp], args*)
    failure.foreach(error => throw IllegalStateException("Header minimum failed", error))

final class HeaderMinimumApp extends Application:
  import HeaderMinimumProbe.*
  private var dock: Option[Dock]                                    = None
  private var appearance: Option[MeridianAppearance]                = None
  private var binding: Option[MeridianDock]                         = None
  private var registration: Option[MeridianAppearance#Registration] = None
  private var window: Option[Stage]                                 = None
  private var destination                                           = Path.of(".")
  private var phase                                                 = 0
  private var builds                                                = 0
  private var cleaned                                               = false
  private var closedRegistrations                                   = -1
  private var disposals                                             = 0
  private var original: Option[LayoutState]                         = None
  private val reports = scala.collection.mutable.ArrayBuffer.empty[ujson.Value]
  private val cases =
    for
      minimum <- Vector(32.0, 0.0, 48.0)
      theme   <- Vector(Appearance.LIGHT, Appearance.DARK)
      density <- Vector(DensityMode.COMPACT, DensityMode.COMFORTABLE)
      scale   <- Vector(1.0, 1.5, 2.0)
    yield (minimum, theme, density, scale)
  private def value = dock.get
  private def app   = appearance.get
  private def later(body: => Unit): Unit =
    val pause = new PauseTransition(Duration.millis(100))
    pause.setOnFinished(_ =>
      try body
      catch case error: Throwable => fail(error)
    )
    pause.play()

  override def start(stage: Stage): Unit =
    window = Some(stage)
    try
      require(getParameters.getRaw.size == 1, "Pass a new evidence directory")
      destination = Path.of(getParameters.getRaw.get(0))
      Files.createDirectory(destination)
      val factories = PaneFactories.empty.register(Pane): content =>
        builds += 1
        new PaneView[Content]:
          val node: FxNode             = new StackPane(new Label(content.name))
          def snapshot(): Content      = content
          override def dispose(): Unit = disposals += 1
      dock = Some(Dock(
        factories,
        LayoutState.of(column(
          group(Pane(Content("Analysis")).titled("Analysis")),
          group(Pane(Content("Record")).titled("Record"))
        ))
      ))
      original = Some(value.state)
      value.setSettings(value.requestedSettings.copy(edgeBandPx = 19, defaultMinPanePx = 91))
      appearance = Some(MeridianAppearance.install())
      val scene = new Scene(value.view, 600, 400)
      registration = Some(app.register(scene))
      stage.setScene(scene)
      stage.show()
      val before = value.requestedSettings
      for invalid <- Vector(-1.0, Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity) do
        var refused = false
        try MeridianDock.bind(value, app, invalid)
        catch case _: IllegalArgumentException => refused = true
        require(
          refused && value.requestedSettings == before,
          "Invalid minimum changed presentation"
        )
      prepare()
    catch case error: Throwable => fail(error)

  private def headers: Vector[FxNode] =
    def descend(node: FxNode): Vector[FxNode] =
      Vector(node) ++ (node match
        case parent: Parent => parent.getChildrenUnmodifiable.asScala.toVector.flatMap(descend)
        case _              => Vector.empty)
    descend(value.view).filter(_.getStyleClass.contains("dock-header"))

  private def prepare(): Unit =
    val (minimum, theme, density, scale) = cases(phase)
    if phase % 12 == 0 then
      binding.foreach: old =>
        old.close(); old.close()
        require(old.subscriptionCount == 0, "Unbinding leaked a subscription")
      binding = Some(if minimum == 0 then MeridianDock.bind(value, app)
      else MeridianDock.bind(value, app, minimum))
    app.setPreferences(new MeridianPreferences(theme, density, scale, Motion.REDUCED))
    later:
      val expected = math.max(minimum, app.metrics().headerMinHeight())
      val row = ujson.Obj(
        "minimum"    -> minimum,
        "theme"      -> theme.toString,
        "density"    -> density.toString,
        "text_scale" -> scale,
        "expected"   -> expected,
        "requested"  -> value.requestedSettings.headerPx,
        "effective"  -> value.settings.headerPx,
        "headers" -> ujson.Arr.from(
          headers.map(_.getLayoutBounds.getHeight)
        )
      )
      reports += row
      Files.writeString(
        destination.resolve("header-minimum.json"),
        ujson.write(ujson.Arr.from(reports), indent = 2)
      )
      require(headers.size == 2, s"Expected two actual headers, found ${headers.size}")
      require(value.requestedSettings.headerPx == expected, s"Application minimum lost: $row")
      require(value.settings.headerPx >= expected, "Effective header clipped the minimum")
      headers.foreach: header =>
        require(
          math.abs(header.asInstanceOf[Region].getHeight - value.settings.headerPx) < .5,
          "Actual header differs from effective geometry"
        )
      value.view.lookupAll(".dock-tab-title").asScala.foreach: node =>
        require(node.asInstanceOf[Label].getFont.getSize >= 14 * scale, "Typography floor lost")
      require(
        value.state == original.get && builds == 2 && disposals == 0,
        "Preferences replaced layout or pane content"
      )
      require(
        value.requestedSettings.edgeBandPx == 19 && value.requestedSettings.defaultMinPanePx == 91,
        "Binding changed unrelated host metrics"
      )
      phase += 1
      if phase < cases.size then prepare()
      else
        binding.foreach(_.close())
        val closedSettings = value.requestedSettings
        app.setPreferences(new MeridianPreferences(
          Appearance.LIGHT,
          DensityMode.COMPACT,
          1,
          Motion.FULL
        ))
        require(
          value.requestedSettings == closedSettings,
          "Closed binding still receives preferences"
        )
        cleanup()
        require(disposals == builds, "Pane disposal did not balance")
        require(
          binding.get.subscriptionCount == 0 && closedRegistrations == 0,
          "Binding or scene registrations leaked"
        )
        Files.writeString(
          destination.resolve("guards.json"),
          ujson.write(
            ujson.Obj(
              "invalid_minima_refused" -> 4,
              "closed_subscriptions"   -> binding.get.subscriptionCount,
              "closed_registrations"   -> closedRegistrations,
              "pane_builds"            -> builds,
              "pane_disposals"         -> disposals
            ),
            indent = 2
          ) + "\n"
        )

  private def cleanup(): Unit =
    if !cleaned then
      cleaned = true
      binding.foreach(_.close())
      registration.foreach(_.close())
      appearance.foreach(a => closedRegistrations = a.registeredSceneCount())
      dock.foreach(_.dispose())
      appearance.foreach(_.close())
      window.foreach(_.close())
      Platform.exit()
  private def fail(error: Throwable): Unit =
    HeaderMinimumProbe.failure = Some(error)
    error.printStackTrace()
    cleanup()
