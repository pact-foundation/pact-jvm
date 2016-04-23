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

  @SuppressWarnings('FactoryMethodName')
  static VerifierReporter createReporter(String name) {
    def reporter = REPORTERS[name].newInstance()
    if (reporter.hasProperty('name')) {
      reporter.name = name
    }
    reporter
  }

  static List availableReporters() {
    REPORTERS.keySet() as List
  }
}
