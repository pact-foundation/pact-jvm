package au.com.dius.pact.core.model.json

import au.com.dius.pact.core.support.json.JsonParser
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@CompileStatic
//@Ignore
@SuppressWarnings('ExplicitCallToDivMethod')
class JsonPerformanceSpec {

  private final Map<String, String> jsonFiles = [:]

  @Before
  void setup() {
    def resource = JsonPerformanceSpec.getResource('/v1-pact.json')
    def file = new File(resource.toURI())
    file.parentFile.eachFile { f ->
      if (f.file && f.name.endsWith('.json')) {
        jsonFiles[f.name] = f.text
      }
    }

    resource.openStream().withCloseable {
      JsonParser.INSTANCE.parseStream(it)
    }
    resource.openStream().withCloseable {
      com.google.gson.JsonParser.parseReader(it.newReader())
    }
    resource.openStream().withCloseable {
     new JsonSlurper().parse(it)
    }
  }

  @Test
  void 'test Pact JSON parser'() {
    Map<String, BigInteger> result = [:]
    jsonFiles.each { entry ->
      result[entry.key] = BigInteger.ZERO
    }

    100.times {
      jsonFiles.each { entry ->
        long start = System.nanoTime()
        JsonParser.INSTANCE.parseString(entry.value)
        long time = System.nanoTime() - start
        result[entry.key] += time.toBigInteger()
      }
    }

    println 'RESULT:'
    BigInteger total = 0
    result.keySet().toSorted().each { key ->
      println("${key.padRight(40)}: ${result[key] / 100}")
      total += result[key].div(100).toBigInteger()
    }
    println("${'TOTAL'.padRight(40)}: ${total}")
    println()
  }

  @Test
  void 'test GSON parser'() {
    Map<String, BigInteger> result = [:]
    jsonFiles.each { entry ->
      result[entry.key] = BigInteger.ZERO
    }

    100.times {
      jsonFiles.each { entry ->
        long start = System.nanoTime()
        com.google.gson.JsonParser.parseString(entry.value)
        long time = System.nanoTime() - start
        result[entry.key] += time.toBigInteger()
      }
    }

    println 'RESULT:'
    BigInteger total = 0
    result.keySet().toSorted().each { key ->
      println("${key.padRight(40)}: ${result[key] / 100}")
      total += result[key].div(100).toBigInteger()
    }
    println("${'TOTAL'.padRight(40)}: ${total}")
    println()
  }

  @Test
  void 'test Groovy Json slurper parser'() {
    JsonSlurper slurper = new JsonSlurper()
    Map<String, BigInteger> result = [:]
    jsonFiles.each { entry ->
      result[entry.key] = BigInteger.ZERO
    }

    100.times {
      jsonFiles.each { entry ->
        long start = System.nanoTime()
        slurper.parseText(entry.value)
        long time = System.nanoTime() - start
        result[entry.key] += time.toBigInteger()
      }
    }

    println 'RESULT:'
    BigInteger total = 0
    result.keySet().toSorted().each { key ->
      println("${key.padRight(40)}: ${result[key] / 100}")
      total += result[key].div(100).toBigInteger()
    }
    println("${'TOTAL'.padRight(40)}: ${total}")
    println()
  }
}
