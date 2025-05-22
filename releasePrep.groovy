#!/usr/bin/env groovy
@Grab(group = 'com.github.zafarkhaja', module = 'java-semver', version = '0.9.0')
@Grab(group = 'com.vdurmont', module = 'semver4j', version = '3.1.0')
import com.github.zafarkhaja.semver.Version
import com.vdurmont.semver4j.Semver

def executeOnShell(String command, Closure closure = null) {
  executeOnShell(command, new File(System.properties.'user.dir'), closure)
}

def executeOnShell(String command, File workingDir, Closure closure = null) {
  println "==>: $command"
  def processBuilder = new ProcessBuilder(['sh', '-c', command])
    .directory(workingDir)

  if (closure) {
    processBuilder.redirectErrorStream(true)
  } else {
    processBuilder.inheritIO()
  }
  def process = processBuilder.start()
  if (closure) {
    process.inputStream.eachLine closure
  }
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

def javaVersion
executeOnShell("./gradlew --version 2>/dev/null | awk '/^Launcher JVM:/ { print \$3 }'") {
  javaVersion = new Semver(it.trim().replace('_', '+b'), Semver.SemverType.NPM)
}

if (!javaVersion?.satisfies('>=17.0.0')) {
  ask("You are building against Java $javaVersion and this build requires JDK 17+. Do you want to exit?: [Y]") {
    System.exit(1)
  }
}

ask('Execute Build?: [Y]') {
  executeOnShell './gradlew clean build'
}

def projectProps = './gradlew :core:model:properties'.execute().text.split('\n').inject([:]) { acc, v ->
  if (v ==~ /\w+: .*/) {
    def kv = v.split(':')
    acc[kv[0].trim()] = kv[1].trim()
  }
  acc
}

def version = projectProps.version

def prevTag = 'git describe --abbrev=0  --tags --match=4_7_*'.execute().text.trim()
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
  executeOnShell("git diff --cached")
  executeOnShell("git commit -m 'update changelog for release $releaseVer'")
  executeOnShell("git status")
}

ask('Tag and Push commits?: [Y]') {
  executeOnShell 'git push'
  executeOnShell("git tag ${releaseVer.replaceAll('\\.', '_')}")
  executeOnShell 'git push --tags'
}

ask('Publish artifacts to maven central?: [Y]') {
  executeOnShell './gradlew clean publish -S -x :provider:gradle:publish -PisRelease=true'
  executeOnShell './gradlew :provider:gradle:publishPluginMavenPublicationToMavenRepository -PisRelease=true'
}

ask('Publish Gradle plugin?: [Y]') {
  executeOnShell './gradlew :provider:gradle:publishPlugins -PisRelease=true'
}

ask('Publish pacts to pact-foundation.pactflow.io?: [Y]') {
  executeOnShell 'PACT_PUBLISH=true ./gradlew :pact-publish:test :pact-publish:pactPublish'
}

def nextVer = Version.valueOf(releaseVer).incrementPreReleaseVersion()
ask("Bump version to $nextVer?: [Y]") {
  executeOnShell "sed -i -e \"s/version = '${releaseVer}'/version = '${nextVer}'/\" buildSrc/src/main/groovy/au.com.dius.pact.kotlin-common-conventions.gradle"
  executeOnShell("git add buildSrc/src/main/groovy/au.com.dius.pact.kotlin-common-conventions.gradle")
  executeOnShell("git diff --cached")
  ask("Commit and push this change?: [Y]") {
    executeOnShell("git commit -m 'bump version to $nextVer'")
    executeOnShell("git push")
  }
}
