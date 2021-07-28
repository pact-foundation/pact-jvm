package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Utils
import au.com.dius.pact.core.support.json.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import java.io.File
import java.util.Collections
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties

/**
 * Base Pact class
 */
abstract class BasePact @JvmOverloads constructor(
  override var consumer: Consumer,
  override var provider: Provider,
  override val metadata: Map<String, Any?> = DEFAULT_METADATA,
  override val source: PactSource = UnknownPactSource
) : Pact {

  override fun write(pactDir: String, pactSpecVersion: PactSpecVersion): Result<Int, Throwable> {
    return DefaultPactWriter.writePact(fileForPact(pactDir), this, pactSpecVersion)
  }

  open fun fileForPact(pactDir: String) = File(pactDir, "${consumer.name}-${provider.name}.json")

  override fun compatibleTo(other: Pact): Result<Boolean, String> {
    return if (provider != other.provider) {
      Err("Provider names are different: '$provider' and '${other.provider}'")
    } else if (!this::class.java.isAssignableFrom(other::class.java)) {
      Err("Pact types different: '${other::class.simpleName}' can not be assigned to '${this::class.simpleName}'")
    } else {
      Ok(true)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BasePact

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

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(interactions.flatMap { it.validateForVersion(pactVersion) })
    return errors
  }

  companion object : KLogging() {
    @JvmStatic
    val DEFAULT_METADATA: Map<String, Map<String, Any?>> by lazy {
      Collections.unmodifiableMap(mapOf(
        "pactSpecification" to mapOf("version" to "4.0"),
        "pact-jvm" to mapOf("version" to lookupVersion())
      ))
    }

    @JvmStatic
    fun metaData(metadata: JsonValue?, pactSpecVersion: PactSpecVersion): Map<String, Any?> {
      val pactJvmMetadata = mutableMapOf<String, Any>("version" to lookupVersion())
      val updatedToggles = FeatureToggles.updatedToggles()
      if (updatedToggles.isNotEmpty()) {
        pactJvmMetadata["features"] = updatedToggles
      }
      return Json.toMap(metadata) + mapOf(
        "pactSpecification" to mapOf("version" to pactSpecVersion.versionString()),
        "pact-jvm" to pactJvmMetadata
      )
    }

    @JvmStatic
    fun lookupVersion(): String {
      return Utils.lookupVersion(BasePact::class.java)
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
