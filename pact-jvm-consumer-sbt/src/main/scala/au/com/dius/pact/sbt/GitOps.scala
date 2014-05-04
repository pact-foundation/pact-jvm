package au.com.dius.pact.sbt

import java.io.File
import sbt.Logger
import sbt.IO
import org.eclipse.jgit.api.{Status, Git, CloneCommand}
import scala.util.{Success, Failure, Try}
import org.eclipse.jgit.dircache.DirCache

object GitOps {

  case object FakeException extends Exception

  def pushPact(pactFile: File, providerPactDir: String, repoUrl: String, targetDir: File, log: Logger, dryRun: Boolean): GitResult[String] = {
    log.info("cleaning...")
    val repoDirName = repoUrl.split("/").last.replaceAll(".git", "")
    val repoDir = new File(targetDir, repoDirName)
    IO.delete(repoDir)
    log.info("cloning ...")

    def copyPacts {
      val pactsDir = new File(repoDir, providerPactDir)
      IO.createDirectory(new File(repoDir, providerPactDir))
      val pactName = pactFile.getName
      IO.copyFile(pactFile, new File(pactsDir, pactName))
    }

    def somethingToCommit(status: Status): Try[GitResult[String]] = {
      if(status.isClean) {
        Failure(FakeException)
      } else {
        Success(HappyResult(s"not clean"))
      }
    }

    def invokePush(git:Git): Try[GitResult[String]] = {
      if(dryRun) {
        Success(HappyResult("Pact committed without pushing"))
      } else {
        Try(git.push().call()).map(_ => HappyResult("Pact Pushed"))
      }
    }

    val cmd = new CloneCommand().setDirectory(repoDir).setURI(repoUrl)
    val tryResult = for {
      git <- Try(cmd.call())
      _ = copyPacts
      status <- Try(git.status().call())
      _ <- somethingToCommit(status)
      added <- Try(git.add().addFilepattern(providerPactDir).call())
      _ <- Try(git.commit().setMessage("updated pact").call())
      result <-  invokePush(git)
    } yield result

    tryResult match {
      case Success(r) => r
      case Failure(FakeException) => HappyResult("nothing to commit")
      case Failure(t) => FailedResult(t.getMessage)
    }
  }
}
