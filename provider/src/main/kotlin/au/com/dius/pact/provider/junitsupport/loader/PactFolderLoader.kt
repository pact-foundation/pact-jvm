package au.com.dius.pact.provider.junitsupport.loader

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import java.io.File
import java.net.URLDecoder
import kotlin.reflect.KClass

/**
 * Out-of-the-box implementation of [PactLoader]
 * that loads pacts from either a subfolder of project resource folder or a directory
 */
class PactFolderLoader : PactLoader {

  private val path: File
  private val pactSource: DirectorySource

  @JvmOverloads
  constructor(
    path: String,
    valueResolverClass: KClass<out ValueResolver>? = null,
    valueResolver: ValueResolver? = null,
    ep: ExpressionParser = ExpressionParser(),
  ) {
    val resolver = setupValueResolver(valueResolver, valueResolverClass)
    val interpolatedPath = ep.parseExpression(path, DataType.STRING, resolver) as String
    this.path = File(interpolatedPath)
    this.pactSource = DirectorySource(this.path)
  }

  constructor(pactFolder: PactFolder) : this(
    pactFolder.value,
    pactFolder.valueResolver
  )

  constructor(path: File) {
    this.path = path
    this.pactSource = DirectorySource(this.path)
  }

  override fun description() = "Directory(${pactSource.dir})"

  override fun load(providerName: String): List<Pact> {
    val pacts = mutableListOf<Pact>()
    val pactFolder = resolvePath()
    val files = pactFolder.listFiles { _, name -> name.endsWith(".json") }
    if (files != null) {
      for (file in files) {
        val pact = DefaultPactReader.loadPact(file)
        if (pact.provider.name == providerName) {
          pacts.add(pact)
          this.pactSource.pacts.put(file, pact)
        }
      }
    }
    return pacts
  }

  override fun getPactSource() = this.pactSource

  private fun resolvePath(): File {
    val resourcePath = PactFolderLoader::class.java.classLoader.getResource(path.path)
    return if (resourcePath != null) {
      File(URLDecoder.decode(resourcePath.path, "UTF-8"))
    } else {
      return path
    }
  }

  private fun setupValueResolver(
    valueResolver: ValueResolver?,
    valueResolverClass: KClass<out ValueResolver>?,
  ): ValueResolver {
    var resolver: ValueResolver = valueResolver ?: SystemPropertyResolver
    if (valueResolverClass != null) {
      if (valueResolverClass.objectInstance != null) {
        resolver = valueResolverClass.objectInstance!!
      } else {
        try {
          resolver = valueResolverClass.java.newInstance()
        } catch (e: InstantiationException) {
          PactBrokerLoader.logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
        } catch (e: IllegalAccessException) {
          PactBrokerLoader.logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
        }
      }
    }
    return resolver
  }

}
