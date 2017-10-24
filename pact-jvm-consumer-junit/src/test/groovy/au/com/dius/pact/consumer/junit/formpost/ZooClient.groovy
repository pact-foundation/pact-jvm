package au.com.dius.pact.consumer.junit.formpost

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder

class ZooClient {
  private final String url

  ZooClient(String url) {
    this.url = url
  }

  Animal saveAnimal(String type, String name) {
    def http = new HTTPBuilder(url)
    def response = http.post(path: '/zoo-ws/animals', body: [type: type, name: name],
      requestContentType: ContentType.URLENC)
    new Animal(response)
  }
}
