package au.com.dius.pact.consumer.junit.formpost

import groovyx.net.http.ContentTypes
import groovyx.net.http.HttpBuilder

class ZooClient {
  private final String url

  ZooClient(String url) {
    this.url = url
  }

  Animal saveAnimal(String type, String name) {
    def http = HttpBuilder.configure { request.uri = url }
    def response = http.post {
      request.uri.path = '/zoo-ws/animals'
      request.body = [type: type, name: name]
      request.contentType = ContentTypes.URLENC[0]
    }
    new Animal(response)
  }

  Animal saveAnimal(String type, String name, String level) {
    def http = HttpBuilder.configure { request.uri = url }
    def response = http.post {
      request.uri.path = '/zoo-ws/animals'
      request.uri.query = [level: level]
      request.body = [type: type, name: name]
      request.contentType = ContentTypes.URLENC[0]
    }
    new Animal(response)
  }
}
