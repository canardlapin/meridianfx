import java.net.URI
import scala.sys.process._

val javafxVersion     = "27"
val atlantafxVersion  = "3.0.0"
val scaladockRevision = "4a7a61bb397a68d5048f809a6ea1c30ce8b45223"

ThisBuild / organization   := "io.github.canardlapin"
ThisBuild / scalaVersion   := "3.7.4"
ThisBuild / version        := "0.1.0-dev.1"
ThisBuild / licenses       := Seq(License.MIT)
ThisBuild / publish / skip := true

val fxPlatform = (sys.props("os.name"), sys.props("os.arch")) match {
  case (os, "aarch64") if os.startsWith("Mac")   => "mac-aarch64"
  case (os, _) if os.startsWith("Mac")           => "mac"
  case (os, "aarch64") if os.startsWith("Linux") => "linux-aarch64"
  case (os, _) if os.startsWith("Linux")         => "linux"
  case (os, "aarch64") if os.startsWith("Windows") =>
    sys.error("Windows ARM64 is not a qualified Meridian platform.")
  case (os, _) if os.startsWith("Windows") => "win"
  case other                               => sys.error(s"Unsupported JavaFX platform: $other")
}

val javafxModules = Seq("base", "graphics", "controls")
val javafxDeps = javafxModules.map(m =>
  ("org.openjfx" % s"javafx-$m" % javafxVersion).classifier(fxPlatform)
)

// The default is a remotely retrievable full source SHA. Local development is opt-in.
val scaladockBuild: URI = sys.props.get("meridian.scaladock.path") match {
  case Some(path) =>
    val checkout = file(path).getCanonicalFile
    require((checkout / "build.sbt").isFile, s"Not a ScalaDock checkout: $checkout")
    checkout.toURI
  case None =>
    uri(s"https://github.com/canardlapin/scaladock.git#$scaladockRevision")
}
lazy val scaladockFx = ProjectRef(scaladockBuild, "fx")

lazy val writeFoundationReport = taskKey[File]("Write resolved dependencies and runtime classpath.")
lazy val foundationReport      = taskKey[Seq[File]]("Generate all module foundation reports.")
lazy val providerResolutionReport =
  taskKey[File]("Record the actual ScalaDock source checkout used by sbt.")

val commonSettings = Seq(
  javacOptions ++= Seq("--release", "25", "-Xlint:all"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all"),
  libraryDependencies ++= javafxDeps,
  dependencyOverrides ++= javafxModules.map(m => "org.openjfx" % s"javafx-$m" % javafxVersion),
  writeFoundationReport := {
    val out = target.value / "foundation"
    IO.createDirectory(out)
    val classpath = (Runtime / fullClasspath).value.files.map(_.getCanonicalPath).sorted
    IO.write(out / "runtime-classpath.txt", classpath.mkString("", "\n", "\n"))
    val resolved = update.value.configurations.flatMap(c =>
      c.modules.map(m =>
        s"${c.configuration}\t${m.module.organization}\t${m.module.name}\t${m.module.revision}"
      )
    ).distinct.sorted
    IO.write(out / "resolved-dependencies.tsv", resolved.mkString("", "\n", "\n"))
    streams.value.log.info(s"Wrote foundation report: $out")
    out
  }
)

lazy val theme = project.in(file("modules/theme"))
  .settings(commonSettings)
  .settings(
    name                                    := "meridian-theme",
    autoScalaLibrary                        := false,
    crossPaths                              := false,
    libraryDependencies += "io.github.mkpaz" % "atlantafx-base" % atlantafxVersion,
    Compile / resourceGenerators += Def.task {
      val repo = (LocalRootProject / baseDirectory).value
      val exit =
        Process(Seq("python3", (repo / "tools/build_themes.py").getPath, "--verify"), repo).!
      require(exit == 0, "Stale Meridian theme resources; regenerate with tools/build_themes.py")
      Seq.empty[File]
    }.taskValue
  )

lazy val workbench = project.in(file("modules/workbench"))
  .dependsOn(theme)
  .settings(commonSettings)
  .settings(name := "meridian-workbench", autoScalaLibrary := false, crossPaths := false)

lazy val scaladock = project.in(file("modules/scaladock"))
  .dependsOn(theme, scaladockFx)
  .settings(commonSettings)
  .settings(
    name := "meridian-scaladock",
    providerResolutionReport := {
      val provider           = (scaladockFx / baseDirectory).value
      def git(args: String*) = Process(Seq("git") ++ args, provider).!!.trim
      val observed           = git("rev-parse", "HEAD")
      val overridePath       = sys.props.get("meridian.scaladock.path")
      require(
        overridePath.isDefined || observed == scaladockRevision,
        s"Resolved ScalaDock $observed differs from configured $scaladockRevision"
      )
      val out = target.value / "provider-resolution.tsv"
      val rows = Seq(
        "configured"   -> scaladockRevision,
        "uri"          -> scaladockBuild.toString,
        "override"     -> overridePath.getOrElse(""),
        "checkout"     -> git("rev-parse", "--show-toplevel"),
        "observed"     -> observed,
        "tree"         -> git("rev-parse", "HEAD^{tree}"),
        "tracked-diff" -> git("diff", "HEAD", "--stat")
      )
      IO.write(out, rows.map { case (key, value) => s"$key\t$value" }.mkString("", "\n", "\n"))
      streams.value.log.info(
        s"ScalaDock configured=$scaladockRevision observed=$observed checkout=$provider"
      )
      out
    },
    Test / fork          := true,
    Test / baseDirectory := (LocalRootProject / baseDirectory).value,
    Test / javaOptions := {
      val nativeJars = (Test / fullClasspath).value.files.filter(f =>
        f.getName.startsWith("javafx-") && f.getName.endsWith(s"-$fxPlatform.jar")
      ).sortBy(_.getName)
      require(nativeJars.size == javafxModules.size, s"Unexpected JavaFX module path: $nativeJars")
      Seq(
        "--module-path",
        nativeJars.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator),
        "--add-modules",
        "javafx.controls",
        "--enable-native-access=javafx.graphics"
      )
    }
  )

lazy val sampler = project.in(file("modules/sampler"))
  .dependsOn(theme, workbench, scaladock)
  .settings(commonSettings)
  .settings(
    name                          := "meridian-sampler",
    Compile / run / fork          := true,
    Compile / run / baseDirectory := (LocalRootProject / baseDirectory).value,
    Compile / run / javaOptions := {
      val nativeJars = (Runtime / fullClasspath).value.files.filter(f =>
        f.getName.startsWith("javafx-") && f.getName.endsWith(s"-$fxPlatform.jar")
      ).sortBy(_.getName)
      require(nativeJars.size == javafxModules.size, s"Unexpected JavaFX module path: $nativeJars")
      Seq(
        "--module-path",
        nativeJars.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator),
        "--add-modules",
        "javafx.controls",
        "--enable-native-access=javafx.graphics",
        "-Dprism.verbose=true"
      )
    },
    Compile / run / mainClass := Some("meridian.sampler.FoundationSmoke")
  )

lazy val root = project.in(file("."))
  .aggregate(theme, workbench, scaladock, sampler)
  .settings(
    name             := "meridianfx",
    autoScalaLibrary := false,
    crossPaths       := false,
    foundationReport := Seq(
      (theme / writeFoundationReport).value,
      (workbench / writeFoundationReport).value,
      (scaladock / writeFoundationReport).value,
      (sampler / writeFoundationReport).value
    )
  )
