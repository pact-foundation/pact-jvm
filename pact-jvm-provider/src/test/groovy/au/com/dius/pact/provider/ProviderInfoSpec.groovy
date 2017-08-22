package au.com.dius.pact.provider

import spock.lang.Specification

class ProviderInfoSpec extends Specification {

  private ProviderInfo providerInfo
  private File mockPactDir
  private fileList

  def setup() {
    providerInfo = new ProviderInfo()
    fileList = []
    mockPactDir = Mock(File) {
      exists() >> true
      canRead() >> true
      isDirectory() >> true
      listFiles() >> { fileList as File[] }
    }
  }

  def 'returns an empty list if the directory is null'() {
    when:
    def consumers = providerInfo.hasPactsWith('testGroup') {
      pactFileLocation = null
    }

    then:
    consumers == []
  }

  def 'raises an exception if the directory does not exist'() {
    when:
    providerInfo.hasPactsWith('testGroup') {
      pactFileLocation = Mock(File) {
        exists() >> false
      }
    }

    then:
    thrown(RuntimeException)
  }

  def 'raises an exception if the directory is not readable'() {
    when:
    providerInfo.hasPactsWith('testGroup') {
      pactFileLocation = Mock(File) {
        exists() >> true
        canRead() >> false
      }
    }

    then:
    thrown(RuntimeException)
  }

}
