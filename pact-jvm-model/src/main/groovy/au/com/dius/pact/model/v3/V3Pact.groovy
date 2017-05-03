package au.com.dius.pact.model.v3

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

/**
 * Pact implementing V3 version of the spec
 */
@Slf4j
@ToString(includeSuperProperties = true)
@EqualsAndHashCode(callSuper = true)
abstract class V3Pact extends BasePact {

  protected V3Pact(Provider provider, Consumer consumer, Map metadata) {
    super(provider, consumer, metadata)
  }

  void write(String pactDir) {
    super.write(pactDir, PactSpecVersion.V3)
  }

}
