import sbt._

object Dependencies {

  object Version {
    val cats          = "2.7.0"
    val catsEffect    = "3.3.12"
    val catsRetry     = "3.1.0"
    val circe         = "0.14.2"
    val ciris         = "3.0.0"
    val derevo        = "0.13.0"
    val javaxCrypto   = "1.0.1"
    val fs2           = "3.1.3"
    val http4s        = "0.23.1"
    val http4sJwtAuth = "1.0.0"
    val log4cats      = "2.3.1"
    val monocle       = "3.1.0"
    val newtype       = "0.4.4"
    val refined       = "0.9.29"
    val redis4cats    = "1.1.1"
    val skunk         = "0.3.1"
    val squants       = "1.8.3"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
    val logback          = "1.2.11"
    val organizeImports  = "0.6.0"
    val semanticDB       = "4.5.8"

    val weaver = "0.7.12"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% s"circe-$artifact"  % Version.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact            % Version.ciris
    def derevo(artifact: String): ModuleID = "tf.tofu"    %% s"derevo-$artifact" % Version.derevo
    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % Version.http4s

    val cats       = "org.typelevel"    %% "cats-core"   % Version.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % Version.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % Version.catsRetry
    val squants    = "org.typelevel"    %% "squants"     % Version.squants
    val fs2        = "co.fs2"           %% "fs2-core"    % Version.fs2

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val derevoCore  = derevo("core")
    val derevoCats  = derevo("cats")
    val derevoCirce = derevo("circe-magnolia")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce  = http4s("circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Version.http4sJwtAuth

    val monocleCore = "dev.optics" %% "monocle-core" % Version.monocle

    val refinedCore = "eu.timepit" %% "refined"      % Version.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % Version.refined

    val log4cats = "org.typelevel" %% "log4cats-slf4j" % Version.log4cats
    val newtype  = "io.estatico"   %% "newtype"        % Version.newtype

    val javaxCrypto = "javax.xml.crypto" % "jsr105-api" % Version.javaxCrypto

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % Version.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % Version.redis4cats

    val skunkCore  = "org.tpolecat" %% "skunk-core"  % Version.skunk
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % Version.skunk

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % Version.logback

    // Test
    val catsLaws          = "org.typelevel"       %% "cats-laws"          % Version.cats
    val log4catsNoOp      = "org.typelevel"       %% "log4cats-noop"      % Version.log4cats
    val monocleLaw        = "dev.optics"          %% "monocle-law"        % Version.monocle
    val refinedScalacheck = "eu.timepit"          %% "refined-scalacheck" % Version.refined
    val weaverCats        = "com.disneystreaming" %% "weaver-cats"        % Version.weaver
    val weaverDiscipline  = "com.disneystreaming" %% "weaver-discipline"  % Version.weaver
    val weaverScalaCheck  = "com.disneystreaming" %% "weaver-scalacheck"  % Version.weaver

    // Scalafix rules
    val organizeImports = "com.github.liancheng" %% "organize-imports" % Version.organizeImports
  }

  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % Version.betterMonadicFor
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" % "kind-projector" % Version.kindProjector cross CrossVersion.full
    )
    val semanticDB = compilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % Version.semanticDB cross CrossVersion.full
    )
  }

}