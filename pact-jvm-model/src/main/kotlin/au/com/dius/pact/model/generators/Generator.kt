package au.com.dius.pact.model.generators

import au.com.dius.pact.model.PactSpecVersion
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.math.RandomUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

interface Generator {
  fun generate(base: Any?): Any
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any>
}

data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomIntGenerator", "min" to min, "max" to max)
  }

  override fun generate(base: Any?): Any {
    return min + RandomUtils.nextInt(max - min)
  }
}

data class RandomStringGenerator(val size: Int = 20) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomStringGenerator", "size" to size)
  }

  override fun generate(base: Any?): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }
}

class UuidGenerator : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "UuidGenerator")
  }

  override fun generate(base: Any?): Any {
    return UUID.randomUUID().toString()
  }
}

class DateGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "DateGenerator", "format" to this.format)
    }
    return mapOf("type" to "DateGenerator")
  }

  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalDate.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalDate.now().toString()
    }
  }

}

class TimeGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "TimeGenerator", "format" to this.format)
    }
    return mapOf("type" to "TimeGenerator")
  }

  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalTime.now().toString()
    }
  }

}

class DateTimeGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "DateTimeGenerator", "format" to this.format)
    }
    return mapOf("type" to "DateTimeGenerator")
  }

  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalDateTime.now().toString()
    }
  }

}
