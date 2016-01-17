#!/usr/bin/env groovy

if (!args) {
  println 'Release description is required'
  System.exit(1)
}

def executeOnShell(String command, File workingDir = new File(System.properties.'user.dir')) {
  println command
  def process = new ProcessBuilder(['sh', '-c', command])
    .directory(workingDir)
    .redirectErrorStream(true)
    .start()
  process.inputStream.eachLine { println it }
  process.waitFor()
  if (process.exitValue() > 0) {
    System.exit(process.exitValue())
  }
}

executeOnShell 'git merge master'
//executeOnShell './gradlew clean check install'

//VERSION=$(cat build.gradle| awk "/version = '[0-9]+\.[0-9]+\.[0-9]+'/{ match(\$0, /[0-9]+\.[0-9]+\.[0-9]+/); print substr(\$0, RSTART, RLENGTH) }")
//PREV_TAG=$(git describe --abbrev=0 --tags)
//CHANGELOG=$(git log --pretty='* %h - %s (%an, %ad)' $PREV_TAG..HEAD | tr '\n' '#n')
//
//echo $CHANGELOG
//sed -e 1aX -f CHANGELOG.md | head
