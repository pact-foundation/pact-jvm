package au.com.dius.pact.sbt

import org.specs2.mutable.Specification
import java.io.{FileWriter, File}
import sbt.{IO, Logger}
import org.specs2.mock.Mockito
import scala.annotation.tailrec
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
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
    sequential


    def setup(deriveFile: (File, File) => File) = {
      val log = mock[Logger]
      val dryRun = true

      val testPactFile = new File(this.getClass.getClassLoader.getResource("pacts/testpact.txt").getFile)

      @tailrec
      def findTarget(f:File):File = {
        if(f.getName == "target" || f.getName == "build") f else findTarget(new File(f.getParent))
      }

      val targetDir = findTarget(testPactFile)

      val repoDirName = "repo/testRepo"
      val repoDir = new File(targetDir, repoDirName)

      IO.delete(repoDir)

      val repoUrl = repoDir.toURI.toString
      IO.createDirectory(repoDir)
      val providerPactDir = "pacts"
      val pactDir = new File(repoDir, providerPactDir)
      IO.createDirectory(pactDir)
      IO.copyFile(testPactFile, new File(pactDir, testPactFile.getName))

      val git = Git.init().setDirectory(repoDir).call()
      git.add.addFilepattern(".").call().getEntryCount must beGreaterThan(0)
      git.commit.setMessage("init").call()

      GitOps.pushPact(deriveFile(targetDir, testPactFile), providerPactDir, repoUrl, targetDir, log, dryRun)
    }

    "short circuit for no changes" in {
      val result = setup((targetDir, testPactFile) => testPactFile)

      result must beEqualTo(HappyResult("nothing to commit"))
    }

    "work properly when there are changes" in {
      val result = setup((targetDir, testPactFile) => {
        val modifiedPactFile = new File(targetDir, "modifiedPact.txt")
        IO.copyFile(testPactFile, modifiedPactFile)
        writeToFile(modifiedPactFile, "something new")
        modifiedPactFile
      })

      result must beEqualTo(HappyResult("Pact committed without pushing"))
    }
  }
}
