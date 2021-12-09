package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.Utils.lookupVersion
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.fluent.Request
import org.apache.http.message.BasicNameValuePair
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Metric events to send
 */
sealed class MetricEvent {
  /**
   * Verification mode (JUnit test or build tool)
   */
  open fun testFramework(): String? = null

  /**
   * Event name
   */
  abstract fun name(): String

  /**
   * Event category
   */
  abstract fun category(): String

  /**
   * Event action that occurred
   */
  abstract fun action(): String

  /**
   * Value for the event
   */
  abstract fun value(): Int


  /**
   * Consumer test was run (number of interactions)
   */
  data class ConsumerTestRun(
    val numInteractions: Int,
    val testFramework: String
  ): MetricEvent() {
    override fun name() = "Pact consumer tests ran"
    override fun category() = "ConsumerTest"
    override fun action() = "Completed"
    override fun value() = numInteractions
    override fun testFramework() = testFramework
  }

  /**
   * Provider verification test ran (mode Gradle/Maven/Junit etc.)
   */
  data class ProviderVerificationRan(
    val testsRun: Int,
    val testFramework: String
  ): MetricEvent() {
    override fun name() = "Pacts verified"
    override fun category() = "ProviderTest"
    override fun action() = "Completed"
    override fun value() = testsRun
    override fun testFramework() = testFramework
  }
}

/**
 * This sends anonymous metrics to a Google Analytics account. It is used to track usage of JVM and operating system
 * versions. This can be disabled by setting the 'pact_do_not_track' system property or environment variable to 'true'.
 */
object Metrics : KLogging() {
  var warningLogged: Boolean = false

  const val UA_ACCOUNT = "UA-117778936-1"
  const val GA_URL = "https://www.google-analytics.com/collect"

  fun sendMetrics(event: MetricEvent) {
    Thread {
      val doNotTrack = lookupProperty("pact_do_not_track").ifNullOrEmpty {
        System.getenv("pact_do_not_track")
      }
      if (doNotTrack != "true") {
        if (!warningLogged) {
          logger.warn {
            """
            Please note: we are tracking events anonymously to gather important usage statistics like JVM version
            and operating system. To disable tracking, set the 'pact_do_not_track' system property or environment
            variable to 'true'.
            """
          }
          warningLogged = true
        }

        try {
          val osName = lookupProperty("os.name")?.lowercase().orEmpty()
          val osArch = "$osName-${lookupProperty("os.arch")?.lowercase()}"
          val entity = mapOf<String, Any?>(
            "v" to 1,                                               // Version of the API
            "tid" to UA_ACCOUNT,                                    // Property ID
            "uid" to hostnameHash(osName),                          // Anonymous Client ID.
            "an" to "pact-jvm",                                     // App name.
            "aid" to "pact-jvm",                                    // App Id
            "av" to lookupVersion(Metrics::class.java),             // App version.
            "aip" to true,                                          // Anonymise IP address
            "ds" to "client",                                       // Data source
            "cd2" to lookupContext(),                               // Custom Dimension 2: context
            "cd3" to osArch,                                        // Custom Dimension 3: osarch
            "cd6" to event.testFramework(),                         // Custom Dimension 6: test_framework
            "cd7" to lookupProperty("java.runtime.version"),  // Custom Dimension 7: platform_version
            "el" to event.name(),                                   // Event
            "ec" to event.category(),                               // Category
            "ea" to event.action(),                                 // Action
            "ev" to event.value()                                   // Value
          )
            .filterValues { it != null }
            .map {
              BasicNameValuePair(it.key, it.value.toString())
            }
          val response = Request.Post(GA_URL)
            .bodyForm(entity)
            .execute()
            .returnResponse()
          if (response.statusLine.statusCode > 299) {
            logger.debug("Got response from metrics: ${response.statusLine}")
          }
        } catch (ex: Exception) {
          logger.debug(ex) { "Failed to send plugin load metrics" }
        }
      }
    }.start()
  }

  /**
   * This function makes a MD5 hash of the hostname
   */
  private fun hostnameHash(osName: String): String {
    val hostName = if (osName.contains("windows")) {
      lookupEnv("COMPUTERNAME")
    } else {
      lookupEnv("HOSTNAME")
    }
    val hashData = hostName
      .ifNullOrEmpty { execHostnameCommand() }
      .ifNullOrEmpty { uuidNode() }

    return DigestUtils(DigestUtils.getMd5Digest()).digestAsHex(hashData!!.toByteArray())
  }

  private fun uuidNode(): String {
    return UUID.randomUUID().toString()
  }

  private fun execHostnameCommand(): String? {
    val pb = ProcessBuilder("hostname").start()
    pb.waitFor(500, TimeUnit.SECONDS)
    return if (pb.exitValue() == 0) {
      pb.inputStream.bufferedReader().readLine().trim()
    } else {
      // Host name process failed
      null
    }
  }

  private fun lookupProperty(name: String): String? = System.getProperty(name)

  private fun lookupEnv(name: String): String? = System.getenv(name)

  private fun lookupContext(): String {
    return if (CIs.any { lookupEnv(it).isNotEmpty() }) {
      "CI"
    } else {
      "unknown"
    }
  }

  private val CIs = listOf(
    "CI",
    "CONTINUOUS_INTEGRATION",
    "BSTRUSE_BUILD_DIR",
    "APPVEYOR",
    "BUDDY_WORKSPACE_URL",
    "BUILDKITE",
    "CF_BUILD_URL",
    "CIRCLECI",
    "CODEBUILD_BUILD_ARN",
    "CONCOURSE_URL",
    "DRONE",
    "GITLAB_CI",
    "GO_SERVER_URL",
    "JENKINS_URL",
    "PROBO_ENVIRONMENT",
    "SEMAPHORE",
    "SHIPPABLE",
    "TDDIUM",
    "TEAMCITY_VERSION",
    "TF_BUILD",
    "TRAVIS",
    "WERCKER_ROOT"
  )
}
