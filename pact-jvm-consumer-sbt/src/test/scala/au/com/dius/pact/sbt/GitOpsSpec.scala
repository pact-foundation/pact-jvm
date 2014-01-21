package au.com.dius.pact.sbt

import org.specs2.mutable.Specification
import java.io.{FileWriter, File}
import sbt.{IO, Logger}
import org.specs2.mock.Mockito
import com.typesafe.sbt.git.ConsoleGitRunner
import scala.annotation.tailrec

class GitOpsSpec extends Specification with Mockito {

  def writeToFile(file: File, s:String) {
    val fw = new FileWriter(file)
    try {
      fw.write(s)
    } finally {
      fw.close()
    }
  }

  "push pacts" should {
    //todo: clean testRepoDir
    val log = mock[Logger]
    val dryRun = true

    val testPactFile = new File(this.getClass.getClassLoader.getResource("pacts/testpact.txt").getFile)

    @tailrec
    def findTarget(f:File):File = {
      if(f.getName == "target") f else findTarget(new File(f.getParent))
    }

    val targetDir = findTarget(testPactFile)


    val repoDirName = "testRepo"
    val repoDir = new File(targetDir, repoDirName)
    val repoUrl = repoDir.getAbsolutePath
    IO.createDirectory(repoDir)
    val providerPactDir = "pacts"
    val pactDir = new File(repoDir, providerPactDir)
    IO.createDirectory(pactDir)
    println(s"pactDir=$pactDir isDir = ${pactDir.isDirectory}")
    IO.copyFile(testPactFile, new File(pactDir, testPactFile.getName))

    ConsoleGitRunner("init")(repoDir)
    ConsoleGitRunner("add", ".")(repoDir)
    ConsoleGitRunner("commit", "-am", "init")(repoDir)



    "short circuit for no changes" in {

      val result = GitOps.pushPact(testPactFile, providerPactDir, repoUrl, targetDir, log, dryRun)

      result must beEqualTo(TerminatingResult("nothing to commit"))
    }
  }
}
