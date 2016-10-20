package au.com.dius.pact.provider.reporters

/**
 * Manages the available verifier reporters
 */
class ReporterManager {
  private static final Map<String, Class> REPORTERS = [
    console: AnsiConsoleReporter,
    markdown: MarkdownReporter,
    json: JsonReporter
  ]

  static boolean reporterDefined(String name) {
    REPORTERS.containsKey(name)
  }

  @SuppressWarnings(['FactoryMethodName', 'ThrowRuntimeException'])
  static VerifierReporter createReporter(String name) {

    def reporter

    if (reporterDefined(name)) {

      reporter = REPORTERS[name].newInstance()

    } else {
      // maybe name is a fully qualified name
      try {
        def loader = ReporterManager.classLoader
        def instance = loader.loadClass(name)?.newInstance()

        if (instance == null) {
          throw new IllegalArgumentException("No reporter with name '$name' at classpath")
        }

        if (! VerifierReporter.isAssignableFrom(instance.getClass())) {
          throw new IllegalArgumentException("Reporter with name '$name' does not implement VerifierReporter")
        }

        reporter = instance

      } catch (e) {
        throw new IllegalArgumentException("No reporter with name '$name' defined")
      }
    }

    if (reporter.hasProperty('name')) {
      reporter.name = name
    }
    reporter
  }

  static List availableReporters() {
    REPORTERS.keySet() as List
  }
}
