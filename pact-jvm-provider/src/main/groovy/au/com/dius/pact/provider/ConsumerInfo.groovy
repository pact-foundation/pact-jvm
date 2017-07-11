package au.com.dius.pact.provider

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.FileSource
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.UrlSource
import groovy.transform.Canonical

/**
 * Consumer Info
 */
@Canonical(excludes = ['pactFile'])
class ConsumerInfo {
    String name
    def pactSource
    def stateChange
    boolean stateChangeUsesBody = true
    PactVerification verificationType
    List packagesToScan
    List pactFileAuthentication

    PactSource url(String path) {
      new UrlSource(path)
    }

    Consumer toPactConsumer() {
      new Consumer(name)
    }

  /**
   * Sets the Pact File for the consumer
   * @param file Pact file, either as a string or a PactSource
   * @deprecated Use setPactSource instead
   */
  @Deprecated
  void setPactFile(def file) {
    if (file instanceof PactSource) {
      pactSource = file
    } else {
      pactSource = new FileSource(file as File)
    }
  }

  /**
   * Returns the Pact file for the consumer
   * @deprecated Use getPactSource instead
   */
  @Deprecated
  def getPactFile() {
    pactSource
  }
}
