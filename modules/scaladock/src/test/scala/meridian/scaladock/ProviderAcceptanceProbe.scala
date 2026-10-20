package meridian.scaladock

import java.nio.file.{Files, Path}
import javafx.animation.PauseTransition
import javafx.application.{Application, Platform}
import javafx.event.Event
import javafx.scene.{Node, Scene}
import javafx.scene.control.{ContextMenu, TextField}
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.{Region, StackPane}
import javafx.scene.paint.Color
import javafx.stage.{Stage, Window}
import javafx.util.Duration
import meridian.theme.{MeridianAppearance, MeridianPreferences}
import meridian.theme.MeridianPreferences.{Appearance, Motion}
import meridian.theme.metrics.MeridianMetrics.DensityMode
import _root_.scaladock.*
import _root_.scaladock.dsl.*
import _root_.scaladock.fx.*
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import upickle.default.ReadWriter

/** B0 consumer contract, deliberately outside scaladock.fx and using public APIs only. Appearance
  * forwarding here is explicit fixture wiring; the production adapter is B1.
  */
object ProviderAcceptanceProbe:
  @volatile private[scaladock] var failure: Option[Throwable] = None
  final case class Content(name: String, value: String) derives ReadWriter
  val ContentPane: PaneType[Content] = PaneType[Content]("meridian.b0.synthetic")
  def main(args: Array[String]): Unit =
    Application.launch(classOf[ProviderAcceptanceApp], args*)
    failure.foreach(e => throw IllegalStateException("B0 consumer acceptance failed", e))

final class ProviderAcceptanceApp extends Application:
  import ProviderAcceptanceProbe.*
  private var dock: Option[Dock]                     = None
  private var appearance: Option[MeridianAppearance] = None
  private var owner: Option[Stage]                   = None
  private var output                                 = Path.of(".")
  private var phase                                  = 0
  private var built                                  = 0
  private var disposed                               = 0
  private var layoutEvents                           = 0
  private var menu: Option[ContextMenu]              = None
  private val nodes                                  = mutable.Map.empty[String, TextField]
  private val paneRoots                              = mutable.Map.empty[String, StackPane]
  private val reports                                = mutable.ArrayBuffer.empty[ujson.Value]
  private val hostRoot                               = new StackPane()
  private var leases                                 = Vector.empty[Subscription]
  private val names = Vector(
    "dark-comfortable",
    "light-compact",
    "dark-compact-floating",
    "light-comfortable-restored-scaled"
  )
  private def value: Dock                  = dock.get
  private def paneId(name: String): PaneId = value.state.panes.find(_.title == name).get.id

  override def start(stage: Stage): Unit =
    owner = Some(stage)
    try
      require(getParameters.getRaw.size == 1, "Pass a new evidence directory")
      output = Path.of(getParameters.getRaw.get(0)).toAbsolutePath
      require(!Files.exists(output), "Evidence output must be new")
      Files.createDirectories(output): Unit
      val factories = PaneFactories.empty.register(ContentPane): content =>
        built += 1
        val field = new TextField(content.value)
        nodes(content.name) = field
        val root = new StackPane(field)
        root.setFocusTraversable(true)
        paneRoots(content.name) = root
        new PaneView[Content]:
          val node: Node               = root
          def snapshot(): Content      = content.copy(value = field.getText)
          override def dispose(): Unit = disposed += 1
      def pane(name: String): PaneDef = ContentPane(Content(name, name)).titled(name)
      dock = Some(Dock(
        factories,
        LayoutState.of(row(
          group(pane("Datasets"), pane("Analysis")),
          group(pane("Inspector"), pane("Metadata"))
        ))
      ))
      value.events.subscribe:
        case DockEvent.LayoutChanged(_) => layoutEvents += 1
        case _                          => ()
      appearance = Some(MeridianAppearance.install())
      val scene = new Scene(value.view, 850, 550)
      appearance.get.register(scene)
      stage.setScene(scene)
      stage.setTitle("Meridian B0 synthetic consumer")
      hostRoot.getStyleClass.add("host-owned")
      leases = Vector(value.bindStylesheets(hostRoot), value.bindStylesheets(hostRoot))
      stage.show()
      nodes.foreach((name, node) => node.setText(s"edited:$name"))
      later(keyboard())
    catch case e: Throwable => fail(e)

  private def later(body: => Unit): Unit =
    val pause = new PauseTransition(Duration.millis(350))
    pause.setOnFinished: _ =>
      try body
      catch case e: Throwable => fail(e)
    pause.play()

  private def key(
      target: Node,
      code: KeyCode,
      shift: Boolean = false,
      control: Boolean = false
  ): Unit =
    Event.fireEvent(
      target,
      new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, control, false, false)
    )

  private def keyboard(): Unit =
    value.focus(paneId("Datasets"))
    paneRoots("Datasets").requestFocus()
    require(value.state.focused.contains(paneId("Datasets")), "Initial pane focus missing")
    val consume = new javafx.event.EventHandler[KeyEvent]:
      def handle(event: KeyEvent): Unit = event.consume()
    paneRoots("Datasets").addEventHandler(KeyEvent.KEY_PRESSED, consume)
    key(paneRoots("Datasets"), KeyCode.F6)
    require(value.state.focused.contains(paneId("Datasets")), "Dock stole a content-consumed key")
    paneRoots("Datasets").removeEventHandler(KeyEvent.KEY_PRESSED, consume)
    // The public contract handles keys that bubble from unconsuming content.
    // Use the pane root so TextField's own traversal behavior is not the oracle.
    key(paneRoots("Datasets"), KeyCode.TAB, control = true)
    require(
      value.state.focused.contains(paneId("Analysis")),
      "Ctrl+Tab did not select the next pane"
    )
    key(paneRoots("Analysis"), KeyCode.TAB, shift = true, control = true)
    require(value.state.focused.contains(paneId("Datasets")), "Ctrl+Shift+Tab did not return")
    key(paneRoots("Datasets"), KeyCode.F6)
    require(value.state.focused.contains(paneId("Inspector")), "F6 did not move to the next group")
    key(paneRoots("Inspector"), KeyCode.F6, shift = true)
    require(value.state.focused.contains(paneId("Datasets")), "Shift+F6 did not return")
    value.perform(DockAction.ToggleMaximize)
    require(value.state.maximized.nonEmpty, "Maximize action failed")
    value.perform(DockAction.ToggleMaximize)
    require(value.state.maximized.isEmpty, "Restore action failed")
    println("B0_KEYBOARD_OK actions=6 dispatch=JavaFX-KeyEvent-and-public-action")
    prepare()

  private def prepare(): Unit =
    if phase == 2 then value.popOut(value.state.groupOf(paneId("Inspector")).get.id)
    if phase == 3 then
      val saved = value.save()
      value.dockAllBack()
      require(value.load(saved).isRight, "Saved floating layout did not restore")
    val before       = value.state
    val beforeEvents = layoutEvents
    val dark         = phase % 2 == 0
    val compact      = phase == 1 || phase == 2
    val prefs = new MeridianPreferences(
      if dark then Appearance.DARK else Appearance.LIGHT,
      if compact then DensityMode.COMPACT else DensityMode.COMFORTABLE,
      if phase == 3 then 1.25 else 1.0,
      if compact then Motion.REDUCED else Motion.FULL
    )
    appearance.get.setPreferences(prefs)
    val sheet = getClass.getResource(
      s"/meridian/scaladock/tokens-${if dark then "dark" else "light"}.css"
    ).toExternalForm
    val theme = DockTheme.CustomWithScheme(sheet, dark)
    value.setTheme(theme)
    value.setSettings(LayoutSettings(headerPx = appearance.get.metrics().headerMinHeight()))
    value.setMotion(if appearance.get.reducedMotion() then DockMotion.Reduced else DockMotion.Full)
    require(menu.forall(!_.isShowing), "Presentation change left the old popup showing")
    require(
      value.state == before && layoutEvents == beforeEvents,
      "Presentation changed the layout model"
    )
    require(hostRoot.getStylesheets.contains(sheet), "Bound external root missed the live theme")
    require(
      hostRoot.getStylesheets.asScala.distinct.size == hostRoot.getStylesheets.size,
      "Duplicate bound sheets"
    )
    later(openMenu())

  private def openMenu(): Unit =
    value.focus(paneId("Datasets"))
    nodes("Datasets").requestFocus()
    key(paneRoots("Datasets"), KeyCode.F10, shift = true)
    menu = Window.getWindows.asScala.collectFirst:
      case m: ContextMenu if m.isShowing && m.getOwnerWindow == owner.get => m
    require(menu.exists(_.isShowing), "Shift+F10 did not open the dock's tab menu")
    later(capture())

  private def capture(): Unit =
    val dark       = phase % 2 == 0
    val scheme     = if dark then "DARK" else "LIGHT"
    val mainScene  = owner.get.getScene
    val floatScene = Option(nodes("Inspector").getScene).filter(_ != mainScene)
    require(floatScene.isDefined == (phase >= 2), "Floating/restored scene missing")
    for scene <- Vector(mainScene) ++ floatScene do
      scene.getRoot.applyCss()
      scene.getRoot.layout()
      require(
        scene.getPreferences.getColorScheme.toString == scheme,
        "Scene scheme did not follow custom metadata"
      )
    val popup = menu.get.getScene
    popup.getRoot.applyCss()
    popup.getRoot.layout()
    val expected = Color.web(if dark then "#20232B" else "#FFFFFF")
    val fills = popup.getRoot.lookupAll(".context-menu").asScala.toVector.collect:
      case region: Region if region.getBackground != null =>
        region.getBackground.getFills.asScala.map(_.getFill)
    .flatten
    require(fills.contains(expected), s"Popup palette mismatch: expected $expected, got $fills")
    val requested = appearance.get.metrics().headerMinHeight()
    require(
      value.requestedSettings.headerPx == requested && value.settings.headerPx >= requested,
      "Live header floor lost"
    )
    val headerHeights =
      (Vector(mainScene) ++ floatScene).flatMap(_.getRoot.lookupAll(".dock-header").asScala)
        .filter(_.isVisible).map(_.getLayoutBounds.getHeight)
    require(
      headerHeights.nonEmpty && headerHeights.forall(h =>
        math.abs(h - value.settings.headerPx) < 0.01
      ),
      s"Rendered header heights $headerHeights differ from effective metrics ${value.settings.headerPx}"
    )
    require(
      value.isReducedMotion == appearance.get.reducedMotion(),
      "Dock motion policy differs from Meridian"
    )
    require(built == 4 && disposed == 0, "Presentation rebuilt/disposed a pane")
    require(
      nodes.forall((name, node) => node.getText == s"edited:$name"),
      "Application values changed"
    )
    val snapshot = value.save().toString
    require(
      nodes.keys.forall(name => snapshot.contains(s"edited:$name")),
      "Persistence lost edited pane values"
    )
    writeImage(mainScene, output.resolve(names(phase) + "-main.png"))
    writeImage(popup, output.resolve(names(phase) + "-menu.png"))
    floatScene.foreach(s => writeImage(s, output.resolve(names(phase) + "-float.png")))
    reports += ujson.Obj(
      "phase"           -> names(phase),
      "scheme"          -> scheme,
      "requestedHeader" -> requested,
      "effectiveHeader" -> value.settings.headerPx,
      "renderedHeaders" -> ujson.Arr.from(headerHeights),
      "reducedMotion"   -> value.isReducedMotion,
      "built"           -> built,
      "disposed"        -> disposed,
      "floating"        -> floatScene.isDefined,
      "popupRaised"     -> expected.toString
    )
    println(
      s"B0_PHASE_OK phase=${names(phase)} header=${value.settings.headerPx} scheme=$scheme panes=$built"
    )
    phase += 1
    if phase < names.size then prepare() else finish()

  private def finish(): Unit =
    val before = value.state
    value.setMotion(DockMotion.System)
    require(
      value.isReducedMotion == Platform.getPreferences.isReducedMotion,
      "System motion did not follow JavaFX"
    )
    require(value.state == before, "Motion policy changed the layout")
    leases.head.cancel()
    require(
      hostRoot.getStyleClass.contains("dock"),
      "First cancellation removed a shared style lease"
    )
    leases.last.cancel()
    require(
      !hostRoot.getStyleClass.contains("dock") && hostRoot.getStyleClass.contains(
        "host-owned"
      ) && hostRoot.getStylesheets.isEmpty,
      "Last cancellation did not restore the host root"
    )
    val floatingWindows =
      nodes.values.flatMap(n => Option(n.getScene)).map(_.getWindow).filter(_ != owner.get).toVector
    value.dispose()
    require(
      disposed == 4 && !menu.get.isShowing && floatingWindows.forall(!_.isShowing),
      "Owned resources survived disposal"
    )
    value.dispose()
    require(disposed == 4, "Disposal is not idempotent")
    val runtime =
      ujson.Obj.from(Vector("java.version", "javafx.runtime.version", "os.name", "os.arch").map(k =>
        k -> ujson.Str(System.getProperty(k))
      ))
    runtime("classpath") = ujson.Arr.from(
      System.getProperty("java.class.path").split(java.io.File.pathSeparator).toVector
    )
    runtime("classpathArtifacts") = ujson.Arr.from(
      System.getProperty("java.class.path").split(java.io.File.pathSeparator).toVector.map: entry =>
        val path = Path.of(entry)
        require(Files.isRegularFile(path), s"Expected sbt's packaged runtime artifact: $path")
        ujson.Obj("path" -> entry, "sha256" -> sha256(Files.readAllBytes(path)))
    )
    runtime("providerClassSha256") =
      ujson.Obj.from(Vector(classOf[Dock], classOf[LayoutState]).map: cls =>
        val name   = cls.getName.replace('.', '/') + ".class"
        val stream = cls.getClassLoader.getResourceAsStream(name)
        try name -> ujson.Str(sha256(stream.readAllBytes()))
        finally stream.close())
    runtime("modules") =
      ujson.Arr.from(Vector("javafx.base", "javafx.graphics", "javafx.controls").map: name =>
        val module = ModuleLayer.boot().findModule(name).orElseThrow()
        ujson.Obj(
          "name"    -> name,
          "version" -> module.getDescriptor.rawVersion().orElse("unknown"),
          "location" -> ModuleLayer.boot().configuration().findModule(
            name
          ).orElseThrow().reference().location().orElseThrow().toString
        ))
    Files.writeString(
      output.resolve("result.json"),
      ujson.write(
        ujson.Obj(
          "status"                      -> "pass",
          "runtime"                     -> runtime,
          "phases"                      -> ujson.Arr.from(reports),
          "keyboardActions"             -> 6,
          "keyboardMenus"               -> 4,
          "built"                       -> built,
          "disposed"                    -> disposed,
          "styleLeasesReleased"         -> true,
          "contentConsumedKeyPreserved" -> true,
          "capture" -> "Owned JavaFX scene snapshots; no OS decorations or assistive-technology automation"
        ),
        indent = 2
      ) + "\n"
    ): Unit
    println("B0_CONSUMER_OK phases=4 keyboardActions=6 keyboardMenus=4 panes=4 disposed=4")
    cleanup()

  private def writeImage(scene: Scene, path: Path): Unit =
    val snapshot = scene.snapshot(null)
    val image = new java.awt.image.BufferedImage(
      snapshot.getWidth.toInt,
      snapshot.getHeight.toInt,
      java.awt.image.BufferedImage.TYPE_INT_ARGB
    )
    for y <- 0 until image.getHeight; x <- 0 until image.getWidth do
      image.setRGB(x, y, snapshot.getPixelReader.getArgb(x, y))
    require(javax.imageio.ImageIO.write(image, "png", path.toFile), "PNG writer unavailable")

  private def sha256(bytes: Array[Byte]): String =
    java.util.HexFormat.of().formatHex(
      java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
    )

  private def cleanup(): Unit =
    dock.foreach(_.dispose())
    appearance.foreach(_.close())
    owner.foreach(_.close())
    Platform.exit()

  private def fail(error: Throwable): Unit =
    ProviderAcceptanceProbe.failure = Some(error)
    cleanup()
