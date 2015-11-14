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

//  def 'loading pacts from a directory skips directories'() {
//    given:
//    def mockPactFile = Mock(File) {
//      isDirectory() >> false
//      isFile() >> true
//      getName() >> 'mockPactFile.json'
//    }
//    def mockPactFile2 = Mock(File) {
//      isDirectory() >> false
//      isFile() >> true
//      getName() >> 'mockPactFile2.json'
//    }
//    def mockDir = Mock(File) {
//      isDirectory() >> true
//      isFile() >> false
//      getName() >> 'mockDir'
//    }
//    fileList = [
//      mockDir, mockPactFile, mockPactFile2
//    ]
//
//    when:
//    def consumers = providerInfo.hasPactsWith('testGroup') {
//      pactFileLocation = mockPactDir
//    }
//
//    then:
//    consumers != []
//  }

}
