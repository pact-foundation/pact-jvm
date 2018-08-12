package au.com.dius.pact.provider

import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.ClosurePactSource
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.FileSource
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.UrlSource
import au.com.dius.pact.pactbroker.PactBrokerConsumer
import groovy.transform.Canonical

import java.util.function.Supplier

/**
 * Consumer Info
 */
@Canonical(excludes = ['pactFile'])
class ConsumerInfo implements IConsumerInfo {
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
    } else if (file instanceof Closure) {
      pactSource = new ClosurePactSource(file as Supplier)
    } else if (file instanceof URL) {
      pactSource = new UrlSource(file.toString())
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

  static ConsumerInfo from(PactBrokerConsumer consumer) {
    new ConsumerInfo(name: consumer.name,
      pactSource: new BrokerUrlSource(consumer.source, consumer.pactBrokerUrl, [:], [:], consumer.tag),
      pactFileAuthentication: consumer.pactFileAuthentication)
  }
}
