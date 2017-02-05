package au.com.dius.pact.model.generators

import au.com.dius.pact.model.ContentType
import au.com.dius.pact.model.OptionalBody
import org.apache.commons.lang.math.RandomUtils
import java.util.*

enum class Category {
  METHOD, PATH, HEADER, QUERY, BODY, STATUS
}

interface ContentTypeHandler {
  fun processBody(value: String, fn: (ContentTypeHandler) -> Unit): OptionalBody
  fun applyKey(key: String, generator: Generator)
}

val contentTypeHandlers: Map<String, ContentTypeHandler> = mutableMapOf()

data class Generators(val categories: MutableMap<Category, MutableMap<String, Generator>> = HashMap()) {

  companion object {

    @JvmStatic fun fromMap(map: Map<String, Any>?): Generators {
      return Generators()
    }

  }

  @JvmOverloads
  fun addGenerator(category: Category, key: String? = "", generator: Generator): Generators {
    if (categories.containsKey(category) && categories[category] != null) {
      categories[category]?.put((key ?: ""), generator)
    } else {
      categories[category] = mutableMapOf((key ?: "") to generator)
    }
    return this
  }

  fun applyGenerator(category: Category, closure: (String, Generator?) -> Unit) {
    if (categories.containsKey(category) && categories[category] != null) {
      val categoryValues = categories[category]
      if (categoryValues != null) {
        for ((key, value) in categoryValues) {
          closure.invoke(key, value)
        }
      }
    }
  }

  fun applyBodyGenerators(body: OptionalBody, contentType: ContentType): OptionalBody {
    return when (body.state) {
      OptionalBody.State.EMPTY, OptionalBody.State.MISSING, OptionalBody.State.NULL -> body
      OptionalBody.State.PRESENT -> when {
        contentType.isJson() -> processBody(body.value!!, "application/json")
        contentType.isXml() -> processBody(body.value!!, "application/xml")
        else -> body
      }
    }
  }

  private fun processBody(value: String, contentType: String): OptionalBody {
    val handler = contentTypeHandlers[contentType]
    return handler?.processBody(value) { handler: ContentTypeHandler ->
      applyGenerator(Category.BODY) { key: String, generator: Generator? ->
        if (generator != null) {
          handler.applyKey(key, generator)
        }
      }
    } ?: OptionalBody.body(value)
  }

}

interface Generator {
  fun generate(base: Any?): Any
}

data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun generate(base: Any?): Any {
    return min + RandomUtils.nextInt(max - min)
  }
}

class UuidGenerator : Generator {
  override fun generate(base: Any?): Any {
    return UUID.randomUUID().toString()
  }
}
