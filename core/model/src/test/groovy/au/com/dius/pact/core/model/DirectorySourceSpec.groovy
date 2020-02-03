package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

class DirectorySourceSpec extends Specification {

  @Unroll
  def 'description includes the directory Pacts are contained in'() {
    when:
    def path = new File('/target/pacts')
    def source = new DirectorySource(path)

    then:
    source.description() == "Directory ${path}"
  }
}
