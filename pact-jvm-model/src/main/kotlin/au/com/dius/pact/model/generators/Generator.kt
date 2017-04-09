package au.com.dius.pact.model.generators

import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.math.RandomUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

interface Generator {
  fun generate(base: Any?): Any
}

data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun generate(base: Any?): Any {
    return min + RandomUtils.nextInt(max - min)
  }
}

data class RandomStringGenerator(val size: Int = 20) : Generator {
  override fun generate(base: Any?): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }
}

class UuidGenerator : Generator {
  override fun generate(base: Any?): Any {
    return UUID.randomUUID().toString()
  }
}

class DateGenerator(val format: String? = null) : Generator {
  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalDate.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalDate.now().toString()
    }
  }

}

class TimeGenerator(val format: String? = null) : Generator {
  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalTime.now().toString()
    }
  }

}

class DateTimeGenerator(val format: String? = null) : Generator {
  override fun generate(base: Any?): Any {
    if (format != null) {
      return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      return LocalDateTime.now().toString()
    }
  }

}
