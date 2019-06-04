package au.com.dius.pact.core.model

import mu.KLogging
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.jar.JarInputStream
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties

/**
 * Base Pact class
 */
abstract class BasePact<I> @JvmOverloads constructor(
  override val consumer: Consumer,
  override val provider: Provider,
  open val metadata: Map<String, Any> = DEFAULT_METADATA,
  override val source: PactSource = UnknownPactSource
): Pact<I> where I: Interaction {

  fun write(pactDir: String, pactSpecVersion: PactSpecVersion) {
    PactWriter.writePact(fileForPact(pactDir), this, pactSpecVersion)
  }

  open fun fileForPact(pactDir: String) = File(pactDir, "${consumer.name}-${provider.name}.json")

  override fun compatibleTo(other: Pact<I>) = provider == other.provider &&
    this::class.java.isAssignableFrom(other::class.java)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BasePact<*>

    if (consumer != other.consumer) return false
    if (provider != other.provider) return false

    return true
  }

  override fun hashCode(): Int {
    var result = consumer.hashCode()
    result = 31 * result + provider.hashCode()
    return result
  }

  override fun toString() = "BasePact(consumer=$consumer, provider=$provider, metadata=$metadata, source=$source)"

  companion object: KLogging() {
    val DEFAULT_METADATA: Map<String, Map<String, Any>> = Collections.unmodifiableMap(mapOf(
      "pactSpecification" to mapOf("version" to "3.0.0"),
      "pact-jvm" to mapOf("version" to lookupVersion())
    ))

    @JvmStatic
    fun metaData(pactSpecVersion: PactSpecVersion): Map<String, Any> {
      val pactJvmMetadata = mutableMapOf<String, Any>("version" to lookupVersion())
      val updatedToggles = FeatureToggles.updatedToggles()
      if (updatedToggles.isNotEmpty()) {
        pactJvmMetadata["features"] = updatedToggles
      }
      return mapOf(
        "pactSpecification" to mapOf("version" to if (pactSpecVersion >= PactSpecVersion.V3) "3.0.0" else "2.0.0"),
        "pact-jvm" to pactJvmMetadata
      )
    }

    @JvmStatic
    fun lookupVersion(): String {
      val url = BasePact::class.java.protectionDomain?.codeSource?.location
      return if (url != null) {
        val openStream = url.openStream()
        try {
          val jarStream = JarInputStream(openStream)
          jarStream.manifest?.mainAttributes?.getValue("Implementation-Version") ?: ""
        } catch (e: IOException) {
          logger.warn(e) { "Could not load pact-jvm manifest" }
          ""
        } finally {
          openStream.close()
        }
      } else {
        ""
      }
    }

    fun objectToMap(obj: Any?): Map<String, Any?> {
      return if (obj != null) {
        val toMap = obj::class.declaredFunctions.find { it.name == "toMap" }
        if (toMap != null) {
          toMap.call() as Map<String, Any?>
        } else {
          convertToMap(obj)
        }
      } else
        emptyMap()
    }

    private fun convertToMap(obj: Any): Map<String, Any?> {
      return obj::class.memberProperties.filter { it.name != "class" }.associate { prop ->
        when (val propVal = prop.getter.call(obj)) {
          is Map<*, *> -> prop.name to convertToMap(propVal)
          is Collection<*> -> prop.name to propVal.map { convertToMap(it!!) }
          else -> prop.name to propVal
        }
      }
    }
  }
}
