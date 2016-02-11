#!/usr/bin/env groovy
@Grab(group = 'com.github.zafarkhaja', module = 'java-semver', version = '0.9.0')
import com.github.zafarkhaja.semver.Version

def executeOnShell(String command, Closure closure = null) {
  executeOnShell(command, new File(System.properties.'user.dir'), closure)
}

def executeOnShell(String command, File workingDir, Closure closure = null) {
  println command
  def process = new ProcessBuilder(['sh', '-c', command])
    .directory(workingDir)
    .redirectErrorStream(true)
    .start()
  def cl = closure
  if (cl == null) {
    cl = { println it }
  }
  process.inputStream.eachLine cl
  process.waitFor()
  if (process.exitValue() > 0) {
    System.exit(process.exitValue())
  }
}

void ask(String prompt, String defaultValue = 'Y', Closure cl) {
  def promptValue = System.console().readLine(prompt + ' ').trim()
  if (promptValue.empty) {
    promptValue = defaultValue
  }
  if (promptValue.toUpperCase() == 'Y') {
    cl.call()
  }
}

executeOnShell 'git pull'

ask('Execute Build?: [Y]') {
  executeOnShell './gradlew clean check install'
}

def projectProps = './gradlew :pact-jvm-consumer_2.11:properties'.execute().text.split('\n').inject([:]) { acc, v ->
  if (v ==~ /\w+: .*/) {
    def kv = v.split(':')
    acc[kv[0].trim()] = kv[1].trim()
  }
  acc
}

def version = projectProps.version

def prevTag = 'git describe --abbrev=0 --tags'.execute().text.trim()
def changelog = []
executeOnShell("git log --pretty='* %h - %s (%an, %ad)' ${prevTag}..HEAD".toString()) {
  println it
  changelog << it
}

def releaseDesc = System.console().readLine('Describe this release: [Bugfix Release]').trim()
if (releaseDesc.empty) {
  releaseDesc = 'Bugfix Release'
}

def releaseVer = System.console().readLine("What is the version for this release?: [$version]").trim()
if (releaseVer.empty) {
  releaseVer = version
}

ask('Update Changelog?: [Y]') {
  def changeLogFile = new File('CHANGELOG.md')
  def changeLogFileLines = changeLogFile.readLines()
  changeLogFile.withPrintWriter() { p ->
    p.println(changeLogFileLines[0])

    p.println()
    p.println("# $releaseVer - $releaseDesc")
    p.println()
    changelog.each {
      p.println(it)
    }

    changeLogFileLines[1..-1].each {
      p.println(it)
    }
  }

  executeOnShell("git add CHANGELOG.md")
  executeOnShell("git commit -m 'update changelog for release $releaseVer'")
  executeOnShell("git status")
}

ask('Tag and Push commits?: [Y]') {
  executeOnShell 'git push'
  executeOnShell("git tag ${releaseVer.replaceAll('\\.', '_')}")
  executeOnShell 'git push --tags'
}

ask('Publish artifacts to maven central?: [Y]') {
  executeOnShell './gradlew clean uploadArchives :pact-jvm-provider-gradle_2.11:publishPlugins -S'
}

def nextVer = Version.valueOf(releaseVer).incrementPatchVersion()
ask("Bump version to $nextVer?: [Y]") {
  executeOnShell "sed -i -e \"s/version = '${releaseVer}'/version = '${nextVer}'/\" build.gradle"
  executeOnShell "sed -i -e \"s/def version = \\\"${releaseVer}\\\"/def version = \\\"${nextVer}\\\"/\" project/Build.scala"
  executeOnShell("git add build.gradle project/Build.scala")
  executeOnShell("git diff --cached")
  ask("Commit and push this change?: [Y]") {
    executeOnShell("git commit -m 'bump version to $nextVer'")
    executeOnShell("git push")
  }
}
