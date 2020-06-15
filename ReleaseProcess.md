# Releasing Pact-JVM

The Pact-JVM project releases three types of artifacts: Java JAR files to Maven Central, a Gradle plugin to plugins.gradle.org
and Pact files to pact-foundation.pact.dius.com.au.

## 1. Required Credentials and Software

### Getting access to release to Maven Central

The Maven Central repository is provided by Sonatype. You need to signup to their Jira at 
https://issues.sonatype.org/secure/Signup!default.jspa and then get a Pact-JVM administrator to raise a ticket
to get your user permission to publish to the `au.com.dius` project.

### Access to Gradle plugin portal

You need to get an API key to publish the Pact-JVM Gradle plugin from the current owner of the plugin on the plugin 
portal.

### Getting access to the pact-foundation.pact.dius.com.au broker

You can request access to the broker by emailing support@pactflow.io.

### Create a PGP key

Maven Central has a requirement that all artifacts are signed. You need to create a GPG key for this.

### Install JDK 8

The JAR files must be built with JDK 8. You need to set the JAVA_HOME environment variable to point to this version
of the JDK. 

## 2. Setting up your local Gradle

All the credentials from step 1 need to go into your local gradle property file. For UNIX based systems, this will
be in `~/.gradle/gradle.properties`.

```
sonatypeUsername              # this is the username you signed up with at the Sonatype Jira
sonatypePassword              # this is your sonatype Jira password
gradle.publish.key            # this is the Gradle plugin portal API key
gradle.publish.secret         # this is the Gradle plugin portal API key secret
signing.keyId                 # this is your GPG key ID
signing.password              # GPG key password
signing.secretKeyRingFile     # Path to the GPG secret key file
pactBrokerToken               # This is the API token to pact-foundation.pact.dius.com.au broker
```

## 3. Running the release script

You can run the release script `releasePrep.groovy` in the root of the project. It will prompt you for all
the steps to run. For most of the time you can just hit ENTER for the default values.

If any step fails, you can re-run the script once you have fixed the issue and select `n` for all the steps already run to skip them.
Most of the steps should only ever be run once for a particular version.

## 4. Release the artifacts on Maven Central

Log onto oss.sonatype.org and select `Staging Repositories` from the menu on the left hand side. You should see a new
staging repository for `au.com.dius` listed. Select the repository and select the Close button. This will close the
repository and run the Maven Central rules.

Once the repository is successfully closed (it takes a few minutes), select the repository again and select the Release button.

## 5. Create a Github release

Create a Github release from the tag created by the release script. The contents should be populated with the
values from the CHANGELOG.md that the release script created.
