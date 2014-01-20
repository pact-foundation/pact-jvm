package au.com.dius.pact.sbt

import com.typesafe.sbt.git.ConsoleGitRunner
import java.io.File
import sbt.Logger
import sbt.IO

object GitOps {
  type GitCmd = Seq[String] => GitResult[String]


  private def isError(msg:String) = msg.contains("error")


  private def toGitResult(msg: String): GitResult[String] = {
    if(isError(msg)) FailedResult(msg)
    else HappyResult(msg)
  }


  def withGitRepo(repoDir: File, url: String)(f: GitCmd => GitResult[String]): GitResult[String] = {
    val cloneResult = toGitResult(ConsoleGitRunner("clone", url)(repoDir.getParentFile))

    cloneResult.flatMap { msg =>
      def git(args: Seq[String]): GitResult[String] = { toGitResult(ConsoleGitRunner(args:_*)(repoDir)) }
      f(git)
    }
  }

  def pushPact(pactFile: File, providerPactDir: String, repoUrl: String, targetDir: File, log: Logger, dryRun: Boolean): GitResult[String] = {
    log.info("cleaning...")
    val repoDirName = repoUrl.split("/").last.replaceAll(".git", "")
    val repoDir = new File(targetDir, repoDirName)
    IO.delete(repoDir)
    log.info("cloning ...")

    withGitRepo(repoDir, repoUrl) { g: GitCmd =>
      def git(args: String*) = { g(args) }

      val pactsDir = new File(repoDir, providerPactDir)
      IO.createDirectory(new File(repoDir, providerPactDir))
      val pactName = pactFile.getName
      IO.copyFile(pactFile, new File(pactsDir, pactName))

      for {
        _ <- git("add", providerPactDir)
        _ <-  git("commit", "-m", "updated pact")
        result <-  if(dryRun) HappyResult("Pact committed without pushing") else git("push")
      } yield result
    }
  }
}
