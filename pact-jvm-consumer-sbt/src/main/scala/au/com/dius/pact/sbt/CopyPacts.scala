package au.com.dius.pact.sbt

import sbt._
import Keys.{streams, target}

object CopyPacts extends Plugin {

  val copyPacts = InputKey[Unit]("copy all pacts to provider project, supply dryRun param to skip git push")
  val copyPact = InputKey[Unit]("copy a pact to provider project, supply dryRun param to skip git push")
  val providerRepos = SettingKey[Map[String, String]]("mapping of pact filename to git repo locations")
  val pactFolder = SettingKey[File]("folder that consumer outputs pacts to during test phase")
  val providerPactDir = SettingKey[String]("Location to store pacts in the provider repo")

  val copyPactsSettings = Seq(
    providerPactDir := "src/test/resources/pacts",

    pactFolder := new File(target.value, "pacts"), // <+= Initialize[String](targetValue => new File(targetValue, "pacts")),

    copyPact := {
      import _root_.sbt.complete.DefaultParsers._

      val args : Seq[String] = spaceDelimited("<pactFile>").parsed
      val pactFile: String = args.head

      val dryRun = args.tail.headOption.fold(false)(_ => true)

      val log = streams.value.log

      providerRepos.value.get(pactFile) match {
        case None => sys.error(s"repository not configured for file: $pactFile")
        case Some(repoUrl) =>
          GitOps.pushPact(
            pactFile = new File(pactFolder.value, pactFile),
            providerPactDir = providerPactDir.value,
            repoUrl = repoUrl,
            targetDir = target.value,
            log = log,
            dryRun = dryRun).verifyResult()
      }
    },

    copyPacts := {
      import _root_.sbt.complete.DefaultParsers._

      val args : Seq[String] = spaceDelimited("<dryRun>").parsed

      val dryRun = args.headOption.fold(false)(_ => true)

      val log = streams.value.log
      import scala.collection.JavaConversions._
      pactFolder.value.listFiles().toList.foreach { pactFile: File =>
        providerRepos.value.get(pactFile.getName) match {
          case None => sys.error(s"repository not configured for file: ${pactFile.getName}")
          case Some(repoUrl) => GitOps.pushPact(
            pactFile = pactFile,
            providerPactDir = providerPactDir.value,
            repoUrl = repoUrl,
            targetDir = target.value,
            log = log,
            dryRun = dryRun).verifyResult()
        }
      }
    }
  )


}
