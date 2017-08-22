package au.com.dius.pact.provider.scalatest

import org.scalatest.Tag

object Tags {

  /**
    * Provider pact tests are annotated with this tag by default. Can be excluded or included in the build process.
    */
  object ProviderTest extends Tag("au.com.dius.pact.provider.scalatest.Tags.ProviderTest")

}
