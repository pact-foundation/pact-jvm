package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.model.PactReader
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Out-of-the-box implementation of [PactLoader]
 * that loads pacts from either a subfolder of project resource folder or a directory
 */
class PactFolderLoader<I>(private val path: File) : PactLoader where I: Interaction {
  private val pactSource: DirectorySource<I> = DirectorySource(path)

  constructor(path: String) : this(File(path))

  @Deprecated("Use PactUrlLoader for URLs")
  constructor(path: URL?) : this(if (path == null) "" else path.path)

  constructor(pactFolder: PactFolder) : this(pactFolder.value)

  override fun load(providerName: String): List<Pact<I>> {
    val pacts = mutableListOf<Pact<I>>()
    val pactFolder = resolvePath()
    val files = pactFolder.listFiles { _, name -> name.endsWith(".json") }
    if (files != null) {
      for (file in files) {
        val pact = PactReader.loadPact(file)
        if (pact.provider.name == providerName) {
          pacts.add(pact as Pact<I>)
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
}
