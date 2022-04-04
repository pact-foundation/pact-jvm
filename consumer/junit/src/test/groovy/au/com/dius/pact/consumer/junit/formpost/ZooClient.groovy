package au.com.dius.pact.consumer.junit.formpost

import au.com.dius.pact.core.support.SimpleHttp

class ZooClient {
  private final String url

  ZooClient(String url) {
    this.url = url
  }

  Animal saveAnimal(String type, String name) {
    def http = new SimpleHttp(url)
    def response = http.post('/zoo-ws/animals', "type=$type&name=$name",
      'application/x-www-form-urlencoded')
    new Animal(response.bodyToMap())
  }

  Animal saveAnimal(String type, String name, String level) {
    def http = new SimpleHttp(url)
    def response = http.post("/zoo-ws/animals?level=$level", "type=$type&name=$name",
      'application/x-www-form-urlencoded')
    new Animal(response.bodyToMap())
  }
}
