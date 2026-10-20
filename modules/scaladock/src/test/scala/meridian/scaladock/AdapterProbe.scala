package meridian.scaladock

import java.lang.ref.WeakReference
import java.nio.file.{Files, Path}
import javafx.animation.PauseTransition
import javafx.application.{Application, Platform}
import javafx.css.PseudoClass
import javafx.event.{Event, EventType}
import javafx.scene.{Node as FxNode, Parent, Scene}
import javafx.scene.control.{ContextMenu, Label, TextField}
import javafx.scene.input.{KeyCode, KeyEvent, MouseButton, MouseEvent}
import javafx.scene.layout.{Region, StackPane, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
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

object AdapterProbe:
  @volatile private[scaladock] var failure: Option[Throwable] = None
  final case class Content(name: String, value: String) derives ReadWriter
  val ContentPane: PaneType[Content] = PaneType[Content]("meridian.b1.synthetic")
  def main(args: Array[String]): Unit =
    Application.launch(classOf[AdapterApp], args*)
    failure.foreach(e => throw IllegalStateException("B1 adapter probe failed", e))

final class AdapterApp extends Application:
  import AdapterProbe.*
  private var dock: Option[Dock]                                    = None
  private var appearance: Option[MeridianAppearance]                = None
  private var binding: Option[MeridianDock]                         = None
  private var registration: Option[MeridianAppearance#Registration] = None
  private var owner: Option[Stage]                                  = None
  private var output                                                = Path.of(".")
  private var phase                                                 = 0
  private var built                                                 = 0
  private var disposed                                              = 0
  private var layoutEvents                                          = 0
  private var changes                                               = 0
  private var menu: Option[ContextMenu]                             = None
  private var ghost: Option[Window]                                 = None
  private val fields     = mutable.Map.empty[String, TextField]
  private val roots      = mutable.Map.empty[String, StackPane]
  private val scientific = mutable.ArrayBuffer.empty[Rectangle]
  private val retired    = mutable.ArrayBuffer.empty[WeakReference[Window]]
  private val reports    = mutable.ArrayBuffer.empty[ujson.Value]
  private val names = Vector(
    "dark-comfortable",
    "light-compact",
    "dark-compact-new-float",
    "light-comfortable-scaled-restored"
  )
  private val host                            = new VBox()
  private var hostLease: Option[Subscription] = None
  private var tokenNodes                      = Map.empty[String, Region]
  private val model = ujson.read(Files.readString(Path.of("tokens/meridian.json")))
  private val resolved = ujson.read(
    Files.readString(Path.of("modules/theme/src/main/resources/meridian/theme/tokens.json"))
  )
  private def value: Dock              = dock.get
  private def app: MeridianAppearance  = appearance.get
  private def id(name: String): PaneId = value.state.panes.find(_.title == name).get.id
  private def palette: ujson.Value =
    resolved("appearances")(if app.theme().isDarkMode() then "dark" else "light")
  private def color(role: String): Color = Color.web(palette(role).str)

  override def start(stage: Stage): Unit =
    owner = Some(stage)
    try
      require(getParameters.getRaw.size == 1, "Pass a new capture directory")
      output = Path.of(getParameters.getRaw.get(0)).toAbsolutePath
      require(!Files.exists(output), "Capture directory must be new")
      Files.createDirectories(output): Unit
      val factories = PaneFactories.empty.register(ContentPane): content =>
        built += 1
        val field = new TextField(content.value)
        fields(content.name) = field
        val swatch = new Rectangle(50, 50, Color.MAGENTA)
        scientific += swatch
        val root = new StackPane(new VBox(swatch, field))
        root.setFocusTraversable(true)
        roots(content.name) = root
        new PaneView[Content]:
          val node: FxNode             = root
          def snapshot(): Content      = content.copy(value = field.getText)
          override def dispose(): Unit = disposed += 1
      def pane(name: String): PaneDef = ContentPane(Content(name, name)).titled(name)
      dock = Some(Dock(
        factories,
        LayoutState.of(row(
          group(pane("Datasets"), pane("Analysis")),
          group(pane("Inspector"), pane("Metadata"))
        )),
        settings = LayoutSettings(edgeBandPx = 63, defaultMinPanePx = 51)
      ))
      value.events.subscribe:
        case DockEvent.LayoutChanged(_) => layoutEvents += 1
        case _                          => ()
      appearance = Some(MeridianAppearance.install())
      val scene = new Scene(value.view, 850, 550)
      registration = Some(app.register(scene))
      binding = Some(MeridianDock.bind(value, app))
      try
        MeridianDock.bind(value, app)
        throw IllegalStateException("Duplicate binding was accepted")
      catch case _: IllegalArgumentException => ()
      require(binding.get.subscriptionCount == 1 && app.registeredSceneCount() == 1)
      tokenNodes = model("mappings")("scaladock").obj.keys.map: token =>
        val node = new Region()
        node.setStyle(s"-fx-background-color: $token;")
        node.setMinSize(10, 10)
        host.getChildren.add(node)
        token -> node
      .toMap
      // A detached host scene establishes that bindings do not depend on application ancestry.
      new Scene(host, 40, 200)
      hostLease = Some(value.bindStylesheets(host))
      stage.setScene(scene)
      stage.setTitle("Meridian B1 synthetic adapter")
      stage.show()
      fields.foreach((name, field) => field.setText(s"edited:$name"))
      later(prepare())
    catch case e: Throwable => fail(e)

  private def later(body: => Unit): Unit =
    val pause = new PauseTransition(Duration.millis(250))
    pause.setOnFinished: _ =>
      try body
      catch case e: Throwable => fail(e)
    pause.play()

  private def preferences(
      dark: Boolean,
      compact: Boolean,
      scale: Double = 1,
      reduced: Boolean = false
  ): MeridianPreferences =
    new MeridianPreferences(
      if dark then Appearance.DARK else Appearance.LIGHT,
      if compact then DensityMode.COMPACT else DensityMode.COMFORTABLE,
      scale,
      if reduced then Motion.REDUCED else Motion.FULL
    )

  private def applyPreferences(next: MeridianPreferences): Unit =
    val before         = value.state
    val events         = layoutEvents
    val oldMenu        = menu.filter(_.isShowing)
    val oldPreferences = app.preferences()
    app.setPreferences(next)
    if oldPreferences.appearance() != next.appearance() || oldPreferences.density() != next.density() || oldPreferences.textScale() != next.textScale()
    then
      require(oldMenu.forall(!_.isShowing), "Appearance left an old menu visible")
    require(value.state == before && layoutEvents == events, "Appearance changed persisted layout")
    require(
      value.requestedSettings.edgeBandPx == 63 && value.requestedSettings.defaultMinPanePx == 51,
      "Adapter changed host geometry policy"
    )
    require(value.isReducedMotion == app.reducedMotion(), "Motion was not forwarded")
    assertStyles(value.view)
    assertStyles(host)
    roots.values.flatMap(root => Option(root.getScene)).toSet.foreach: scene =>
      if scene != owner.get.getScene then
        assertStyles(scene.getRoot.lookup(".dock-layout").asInstanceOf[Parent])
    require(
      binding.get.subscriptionCount == 1 && app.registeredSceneCount() == 1,
      "Bindings accumulated"
    )
    require(
      built == 4 && disposed == 0 && scientific.forall(_.getFill == Color.MAGENTA),
      "Domain content changed"
    )
    require(fields.forall((name, field) => field.getText == s"edited:$name"), "Pane edits changed")

  private def prepare(): Unit =
    if phase == 2 then value.popOut(value.state.groupOf(id("Inspector")).get.id)
    if phase == 3 then
      val saved = value.save()
      retired += new WeakReference(roots("Inspector").getScene.getWindow)
      value.dockAllBack()
      require(value.load(saved).isRight, "Float restore failed")
    applyPreferences(preferences(
      phase % 2 == 0,
      phase == 1 || phase == 2,
      if phase == 3 then 1.25 else 1,
      phase % 2 == 1
    ))
    require(menu.forall(!_.isShowing), "Old menu survived presentation change")
    later(openMenu())

  private def key(target: FxNode, code: KeyCode, shift: Boolean = false): Unit =
    Event.fireEvent(
      target,
      new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false)
    )

  private def openMenu(): Unit =
    value.focus(id("Datasets"))
    roots("Datasets").requestFocus()
    val selected =
      value.view.lookupAll(".dock-tab").asScala.find(_.getAccessibleText == "Datasets").get
    selected.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false)
    value.view.applyCss()
    value.setSettings(value.requestedSettings)
    val unfocusedHeight = value.settings.headerPx
    val unfocusedInsets = selected.asInstanceOf[Region].getInsets
    selected.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true)
    value.view.applyCss()
    value.setSettings(value.requestedSettings)
    require(
      value.settings.headerPx == unfocusedHeight,
      s"Focus paint changed header geometry: $unfocusedHeight -> ${value.settings.headerPx}; insets=$unfocusedInsets -> ${selected.asInstanceOf[Region].getInsets}; border=${selected.asInstanceOf[Region].getBorder}"
    )
    require(fills(selected).contains(color("focus.ring")), "Focus ring missing")
    selected.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false)
    key(roots("Datasets"), KeyCode.F10, shift = true)
    menu = Window.getWindows.asScala.collectFirst:
      case m: ContextMenu if m.isShowing && m.getOwnerWindow == owner.get => m
    require(menu.nonEmpty, "Keyboard context menu missing")
    later(checkFrame())

  private def fills(node: FxNode): Vector[javafx.scene.paint.Paint] = node match
    case region: Region if region.getBackground != null =>
      region.getBackground.getFills.asScala.map(_.getFill).toVector
    case _ => Vector.empty

  private def assertStyles(root: Parent): Unit =
    val actual   = root.getStylesheets.asScala.toVector
    val expected = value.theme.stylesheets
    require(
      actual.filterNot(_.endsWith("/*option:meridian.preferences*/")) == expected,
      "Stale or reordered adapter resources survived a change"
    )
    require(
      expected.forall(sheet => actual.count(_ == sheet) == 1),
      "Missing or duplicated dock sheets"
    )
    require(
      actual.indexOf(expected.head) < actual.indexOf(expected.last),
      "Adapter precedes provider CSS"
    )

  private def checkFrame(): Unit =
    val mainScene = owner.get.getScene
    val floating  = Option(roots("Inspector").getScene).filter(_ != mainScene)
    require(floating.nonEmpty == (phase >= 2), "Expected floating scene missing")
    for scene <- Vector(mainScene) ++ floating do
      scene.getRoot.applyCss(); scene.getRoot.layout()
      require(scene.getPreferences.getColorScheme.toString == (if app.theme().isDarkMode() then
                                                                 "DARK"
                                                               else "LIGHT"))
      require(
        scene.getPreferences.isReducedMotion == app.reducedMotion(),
        "Scene motion differs from appearance"
      )
      val groups = scene.getRoot.lookupAll(".dock-group").asScala
      require(
        groups.nonEmpty && groups.forall(g => fills(g).contains(color("surface.canvas"))),
        "Main/float computed surface mismatch"
      )
      val labels = scene.getRoot.lookupAll(".dock-tab-title").asScala.collect { case l: Label => l }
      require(
        labels.nonEmpty && labels.forall(_.getFont.getSize >= 14 * app.preferences().textScale()),
        "Dock text scale lost"
      )
      val headers = scene.getRoot.lookupAll(".dock-header").asScala.filter(_.isVisible)
      require(
        headers.forall(_.getLayoutBounds.getHeight >= app.metrics().headerMinHeight()),
        "Rendered header below density floor"
      )
      val dockRoot = if scene == mainScene then value.view
      else scene.getRoot.lookup(".dock-layout").asInstanceOf[Parent]
      assertStyles(dockRoot)
    val selected = value.view.lookup(".dock-tab:selected")
    require(
      fills(selected).contains(color("selection.inactive-marker")),
      "Selection cue used focus color"
    )
    require(menu.get.isShowing, "Menu closed before computed-paint capture")
    val popup = menu.get.getScene
    popup.getRoot.applyCss(); popup.getRoot.layout()
    // PopupControl's outer root can share this public class without painting a surface.
    val surfaces = popup.getRoot.lookupAll(".context-menu").asScala.map(fills).filter(_.nonEmpty)
    require(
      surfaces.nonEmpty && surfaces.forall(paints =>
        paints.contains(color("surface.raised")) && paints.contains(color("border.control"))
      ),
      s"Popup surface/boundary mismatch: $surfaces"
    )
    // Paint qualification uses an explicit CSS state, independent of OS popup activation timing.
    val focused = popup.getRoot.lookupAll(".menu-item").asScala.find(n =>
      !n.isDisabled && n.lookup(".label") != null
    ).get
    focused.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true)
    popup.getRoot.applyCss()
    require(
      fills(focused).contains(color("selection.active")),
      s"Menu selection recipe missing: ${fills(focused)} expected=${color("selection.active")}"
    )
    require(
      focused.lookup(".label").asInstanceOf[Label].getTextFill == color("text.primary"),
      "Menu selected text contrast role lost"
    )
    host.applyCss(); host.layout()
    tokenNodes.foreach: (token, node) =>
      val role = model("mappings")("scaladock")(token).str
      require(fills(node).contains(color(role)), s"Computed token mismatch: $token -> $role")
    writeImage(mainScene, names(phase) + "-main.png")
    writeImage(popup, names(phase) + "-menu.png")
    floating.foreach(scene => writeImage(scene, names(phase) + "-float.png"))
    menu.get.hide()
    menu = None
    startGhost()

  private def mouse(kind: EventType[MouseEvent], x: Double, y: Double): MouseEvent =
    new MouseEvent(
      kind,
      0,
      0,
      x,
      y,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      true,
      false,
      false,
      true,
      false,
      false,
      null
    )

  private def startGhost(): Unit =
    val tab = value.view.lookupAll(".dock-tab").asScala.find(_.getAccessibleText == "Analysis").get
    val point = tab.localToScreen(5, 5)
    Event.fireEvent(tab, mouse(MouseEvent.MOUSE_PRESSED, point.getX, point.getY))
    Event.fireEvent(tab, mouse(MouseEvent.MOUSE_DRAGGED, point.getX + 25, point.getY + 50))
    ghost = Window.getWindows.asScala.find(w =>
      w.isShowing && w.getScene.getRoot.lookup(".dock-ghost") != null
    )
    require(ghost.nonEmpty, "Real tab drag did not create a ghost")
    later(checkGhost())

  private def checkGhost(): Unit =
    val window = ghost.get
    val root   = window.getScene.getRoot
    root.applyCss(); root.layout()
    val chip = root.lookup(".dock-ghost-chip")
    require(
      fills(chip).contains(color("surface.raised")) && fills(
        chip
      ).contains(color("border.control")),
      "Independent ghost palette/boundary mismatch"
    )
    require(chip.getOpacity == 1, "Ghost reduces essential contrast through opacity")
    require(
      root.lookup(".dock-ghost-title").asInstanceOf[Label].getTextFill == color("text.primary")
    )
    assertStyles(root.lookup(".dock-ghost").asInstanceOf[Parent])
    val p = app.preferences()
    val next = new MeridianPreferences(
      p.appearance(),
      p.density(),
      p.textScale(),
      if app.reducedMotion() then Motion.FULL else Motion.REDUCED
    )
    val before = value.state
    val sheet  = value.theme
    applyPreferences(next)
    require(
      window.isShowing && value.theme == sheet && value.state == before,
      "Motion-only change cancelled the drag"
    )
    writeImage(window.getScene, names(phase) + "-ghost.png")
    key(value.view, KeyCode.ESCAPE)
    require(!window.isShowing, "Cancelled ghost survived")
    ghost = None
    reports += ujson.Obj(
      "phase"                    -> names(phase),
      "tokens"                   -> tokenNodes.size,
      "header"                   -> value.settings.headerPx,
      "font"                     -> (14 * app.preferences().textScale()),
      "mainFloatMenuGhostPaints" -> "pass",
      "motionDragContinuity"     -> "pass"
    )
    println(
      s"B1_FRAME_OK phase=${names(phase)} tokens=${tokenNodes.size} header=${value.settings.headerPx}"
    )
    // Leave a real menu open for the following appearance change to dismiss.
    value.focus(id("Datasets"))
    key(roots("Datasets"), KeyCode.F10, shift = true)
    menu = Window.getWindows.asScala.collectFirst:
      case m: ContextMenu if m.isShowing && m.getOwnerWindow == owner.get => m
    require(menu.nonEmpty, "Menu for appearance-dismissal check did not open")
    phase += 1
    if phase < names.size then prepare() else later(stress())

  private def stress(): Unit =
    for i <- 0 until 100 do
      applyPreferences(preferences(
        i % 2 == 0,
        i % 3 == 0,
        if i % 5 == 0 then 1.25 else 1,
        i % 2 == 1
      ))
      changes += 1
    val saved = value.save().toString
    require(
      fields.keys.forall(name => saved.contains(s"edited:$name")),
      "Persistence lost pane edits"
    )
    retired += new WeakReference(roots("Inspector").getScene.getWindow)
    value.dockAllBack()
    for _ <- 0 until 20 do
      binding.get.close(); binding.get.close()
      require(binding.get.subscriptionCount == 0)
      registration.get.close()
      require(app.registeredSceneCount() == 0)
      registration = Some(app.register(owner.get.getScene))
      binding = Some(MeridianDock.bind(value, app))
      value.popOut(value.state.groupOf(id("Inspector")).get.id)
      retired += new WeakReference(roots("Inspector").getScene.getWindow)
      value.dockAllBack()
      require(
        Window.getWindows.asScala.count(_.isInstanceOf[Stage]) == 1,
        "Closed float still showing"
      )
    app.setPreferences(new MeridianPreferences(
      Appearance.SYSTEM,
      DensityMode.COMFORTABLE,
      1,
      Motion.SYSTEM
    ))
    require(value.isReducedMotion == Platform.getPreferences.isReducedMotion)
    binding.get.close()
    val frozen = value.theme
    app.setPreferences(preferences(!app.theme().isDarkMode(), compact = true))
    require(
      value.theme == frozen && binding.get.subscriptionCount == 0,
      "Unbound adapter still responds"
    )
    registration.get.close()
    require(app.registeredSceneCount() == 0)
    require(scientific.forall(_.getFill == Color.MAGENTA) && built == 4 && disposed == 0)
    hostLease.foreach(_.cancel())
    require(host.getStylesheets.isEmpty, "Host stylesheet lease survived cancellation")
    value.dispose()
    require(disposed == 4)
    println(
      s"B1_STRESS_OK changes=$changes rebinds=20 retiredWindows=${retired.size} subscriptions=0 registrations=0"
    )
    later(checkCollected(0))

  private def checkCollected(attempt: Int): Unit =
    System.gc()
    val alive = retired.count(_.get() != null)
    if alive > 0 && attempt < 12 then later(checkCollected(attempt + 1))
    else
      require(
        alive == 0,
        s"$alive closed floating windows still strongly retained after bounded GC check"
      )
      val report = ujson.Obj(
        "status"          -> "pass",
        "java"            -> System.getProperty("java.version"),
        "javafx"          -> System.getProperty("javafx.runtime.version"),
        "os"              -> System.getProperty("os.name"),
        "arch"            -> System.getProperty("os.arch"),
        "phases"          -> ujson.Arr.from(reports),
        "changes"         -> changes,
        "rebinds"         -> 20,
        "retiredWindows"  -> retired.size,
        "retainedWindows" -> alive,
        "subscriptions"   -> binding.get.subscriptionCount,
        "registrations"   -> app.registeredSceneCount(),
        "built"           -> built,
        "disposed"        -> disposed,
        "capture" -> "Owned scene snapshots and JavaFX event dispatch; synthetic scientific swatches"
      )
      report("modules") =
        ujson.Arr.from(Vector("javafx.base", "javafx.graphics", "javafx.controls").map: name =>
          val module = ModuleLayer.boot().findModule(name).orElseThrow()
          ujson.Obj(
            "name"    -> name,
            "version" -> module.getDescriptor.rawVersion().orElse("unknown")
          ))
      report("runtimeArtifacts") = ujson.Arr.from(
        System.getProperty("java.class.path").split(java.io.File.pathSeparator).toVector.map:
          entry =>
            ujson.Obj("path" -> entry, "sha256" -> sha256(Files.readAllBytes(Path.of(entry))))
      )
      report("providerClassSha256") =
        ujson.Obj.from(Vector(classOf[Dock], classOf[LayoutState]).map: cls =>
          val name   = cls.getName.replace('.', '/') + ".class"
          val stream = cls.getClassLoader.getResourceAsStream(name)
          try name -> ujson.Str(sha256(stream.readAllBytes()))
          finally stream.close())
      Files.writeString(output.resolve("result.json"), ujson.write(report, indent = 2) + "\n"): Unit
      println("B1_ADAPTER_OK changes=100 rebinds=20 retainedWindows=0")
      cleanup()

  private def writeImage(scene: Scene, name: String): Unit =
    val snapshot = scene.snapshot(null)
    val image = new java.awt.image.BufferedImage(
      snapshot.getWidth.toInt,
      snapshot.getHeight.toInt,
      java.awt.image.BufferedImage.TYPE_INT_ARGB
    )
    for y <- 0 until image.getHeight; x <- 0 until image.getWidth do
      image.setRGB(x, y, snapshot.getPixelReader.getArgb(x, y))
    require(javax.imageio.ImageIO.write(image, "png", output.resolve(name).toFile))

  private def sha256(bytes: Array[Byte]): String =
    java.util.HexFormat.of().formatHex(
      java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
    )

  private def cleanup(): Unit =
    binding.foreach(_.close())
    registration.foreach(_.close())
    hostLease.foreach(_.cancel())
    dock.foreach(_.dispose())
    appearance.foreach(_.close())
    owner.foreach(_.close())
    Platform.exit()

  private def fail(error: Throwable): Unit =
    AdapterProbe.failure = Some(error)
    cleanup()
