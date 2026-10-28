package meridian.scaladock

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import javafx.application.Platform
import javafx.beans.InvalidationListener
import meridian.theme.MeridianAppearance
import meridian.theme.metrics.MeridianMetrics
import _root_.scaladock.fx.{Dock, DockMotion, DockTheme}

/** Live appearance for one Dock. Create and close on the JavaFX application thread.
  *
  * The application owns MeridianAppearance and its scene registrations. This binding owns no
  * windows or pane content. Close it before closing the application appearance service; closing
  * stops future updates and leaves the last presentation installed in the dock.
  */
final class MeridianDock private (
    dock: Dock,
    appearance: MeridianAppearance,
    headerMinHeight: Double
) extends AutoCloseable:
  private var closed                        = false
  private val changed: InvalidationListener = _ => refresh()

  private[scaladock] def subscriptionCount: Int = if closed then 0 else 1

  private def start(): Unit =
    refresh()
    appearance.presentationProperty().addListener(changed)

  private def refresh(): Unit =
    MeridianDock.requireFx()
    if !closed then
      val presentation = appearance.presentation()
      val dark         = presentation.theme().isDarkMode()
      val scale        = presentation.preferences().textScale()
      val metrics      = presentation.metrics()
      val theme = DockTheme.CustomWithScheme(MeridianDock.stylesheet(dark, scale, metrics), dark)
      val settings = dock.requestedSettings.copy(
        headerPx = math.max(headerMinHeight, metrics.headerMinHeight()),
        dividerPx = MeridianMetrics.GEOMETRY.divider()
      )
      val motion = if presentation.reducedMotion() then DockMotion.Reduced else DockMotion.Full
      // A motion-only update must not dismiss a drag or rebuild a popup.
      if dock.theme != theme then dock.setTheme(theme)
      dock.setSettings(settings)
      dock.setMotion(motion)

  /** Idempotent unbinding. Also safe after Dock.dispose(); it never mutates the dock. */
  override def close(): Unit =
    MeridianDock.requireFx()
    if !closed then
      closed = true
      appearance.presentationProperty().removeListener(changed)
      dock.view.getProperties.remove(MeridianDock.bindingKey)

object MeridianDock:
  private val bindingKey = new Object()

  /** A dock has one appearance owner. Duplicate live bindings fail before changing presentation. */
  def bind(dock: Dock, appearance: MeridianAppearance): MeridianDock =
    bind(dock, appearance, 0)

  /** Retain an application header/restore-strip minimum in logical pixels across preferences. Zero
    * uses only Meridian's generated density minimum. Text and control preferred sizes can raise the
    * effective height further; this minimum never reduces accessibility floors.
    */
  def bind(dock: Dock, appearance: MeridianAppearance, headerMinHeight: Double): MeridianDock =
    requireFx()
    require(
      headerMinHeight.isFinite && headerMinHeight >= 0,
      "Header minimum must be finite and nonnegative"
    )
    require(dock != null && appearance != null, "A dock and application appearance are required")
    require(
      !dock.view.getProperties.containsKey(bindingKey),
      "This dock already has an appearance binding"
    )
    val binding = new MeridianDock(dock, appearance, headerMinHeight)
    binding.start()
    dock.view.getProperties.put(bindingKey, binding)
    binding

  private def requireFx(): Unit =
    require(Platform.isFxApplicationThread, "MeridianDock requires the JavaFX application thread")

  private def resource(path: String): String =
    Option(
      getClass.getResource(path)
    ).getOrElse(throw IllegalStateException(s"Missing resource: $path")).toExternalForm

  private lazy val palettes: Map[String, Map[String, String]] =
    val stream = getClass.getResourceAsStream("/meridian/theme/tokens.json")
    require(stream != null, "Missing generated Meridian semantic tokens")
    try
      val document = ujson.read(new String(stream.readAllBytes(), UTF_8))
      require(document("schema").str == "meridian.resolved-tokens.v1", "Unsupported token schema")
      document("appearances").obj.iterator.map((mode, roles) =>
        mode -> roles.obj.iterator.map((name, value) => name -> value.str).toMap
      ).toMap
    finally stream.close()

  private def stylesheet(
      dark: Boolean,
      scale: Double,
      metrics: MeridianMetrics.DensityMetrics
  ): String =
    val mode    = if dark then "dark" else "light"
    val tokens  = resource(s"/meridian/scaladock/tokens-$mode.css")
    val recipes = resource("/meridian/scaladock/recipes.css")
    // Define semantic lookups on .dock too: the independent drag ghost need not be a scene root.
    val colors = palettes(mode).toVector.sortBy(_._1).map((role, value) =>
      s"-meridian-${role.replace('.', '-')}: $value;"
    ).mkString("\n")
    val text = MeridianMetrics.TYPOGRAPHY.scaledForText(scale)
    val css = s"""@import url("$tokens");
@import url("$recipes");
.dock { $colors
  -fx-font-size: ${text.general()}px;
  -meridian-dock-target: ${metrics.iconTargetMinSize()}px;
}
.dock .dock-tab-title, .dock .dock-strip-title, .dock .dock-window-title,
.dock .dock-ghost-title, .dock .context-menu .menu-item .label, .dock .tooltip {
  -fx-font-size: ${text.general()}px;
}
.dock .dock-tab-overflow > .label, .dock .dock-maximized-badge {
  -fx-font-size: ${text.metadata()}px;
}
"""
    "data:text/css;base64," + Base64.getEncoder.encodeToString(css.getBytes(UTF_8))
