package au.com.dius.pact.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.JarFile

private val logger = KotlinLogging.logger {}

/**
 * Finds the methods annotated with [PactVerifyProvider] that provide the message for an interaction.
 *
 * Classes that are already known (like the test class) are checked first with reflection. The classpath is only
 * scanned when needed. The scan streams over the class files, and only loads the classes that reference the
 * annotation, so it does not hold the whole classpath in memory.
 */
object ProviderMethodFinder {
  /**
   * System property that turns on verbose logging of the classpath scan
   */
  const val VERBOSE_PROPERTY = "pact.verifier.classpathscan.verbose"

  private val ANNOTATION_NAME = PactVerifyProvider::class.java.name
  private val ANNOTATION_DESCRIPTOR = "L${ANNOTATION_NAME.replace('.', '/')};".toByteArray(Charsets.UTF_8)

  /**
   * Returns the methods annotated with @PactVerifyProvider([description]) on the given classes, including the
   * methods they inherit from superclasses and interfaces. A method that is overridden is only returned once.
   */
  @JvmStatic
  fun findOnClasses(classes: List<Class<*>>, description: String): List<Method> {
    return classes.flatMap { cls ->
      val signatures = mutableSetOf<String>()
      classHierarchy(cls)
        .flatMap { annotatedMethods(it, description) }
        .filter { signatures.add(it.name + it.parameterTypes.joinToString(",", "(", ")") { p -> p.name }) }
    }.distinct()
  }

  /**
   * Scans the classpath for classes that have methods annotated with @PactVerifyProvider. Only classes in the given
   * packages (or their sub-packages) are checked. If no packages are given, all classes are checked. Packages may use
   * `*` as a wildcard.
   *
   * @return The names of the classes that reference the annotation
   */
  @JvmStatic
  fun scanClasspath(classpath: List<URL>, packages: List<String>): List<String> {
    val scanner = Scanner(PackageMatcher(packages), System.getProperty(VERBOSE_PROPERTY) != null)
    classpath.mapNotNull { toFile(it) }.forEach { scanner.scan(it) }
    logger.debug { "Found classes with @PactVerifyProvider methods: ${scanner.candidates}" }
    return scanner.candidates.toList()
  }

  /**
   * Loads the named classes (without initialising them) and returns their methods annotated with
   * @PactVerifyProvider([description]). Classes that fail to load are skipped.
   */
  @JvmStatic
  fun findOnClassNames(classNames: List<String>, classLoader: ClassLoader, description: String): List<Method> {
    return classNames.mapNotNull { loadClass(it, classLoader) }
      .flatMap { annotatedMethods(it, description) }
      .distinct()
  }

  /**
   * Works out the classpath to scan. The URLs are used if given, otherwise the URLs of the class loader. If neither
   * provides any URLs, the classpath of the JVM (`java.class.path`) is used.
   */
  @JvmStatic
  fun classpathFor(classLoader: ClassLoader?, urls: List<URL>): List<URL> {
    if (urls.isNotEmpty()) {
      return urls
    }
    val loaderUrls = generateSequence(classLoader) { it.parent }
      .filterIsInstance<URLClassLoader>()
      .flatMap { it.urLs.asSequence() }
      .toList()
    return loaderUrls.ifEmpty { jvmClasspath() }
  }

  private fun jvmClasspath(): List<URL> {
    return System.getProperty("java.class.path").orEmpty()
      .split(File.pathSeparator)
      .filter { it.isNotBlank() }
      .map { File(it).toURI().toURL() }
  }

  private fun classHierarchy(cls: Class<*>): List<Class<*>> {
    val classes = generateSequence(cls) { it.superclass }
      .takeWhile { it != Any::class.java }
      .toList()
    val interfaces = LinkedHashSet<Class<*>>()
    val queue = ArrayDeque(classes.flatMap { it.interfaces.asList() })
    while (queue.isNotEmpty()) {
      val iface = queue.removeFirst()
      if (interfaces.add(iface)) {
        queue.addAll(iface.interfaces)
      }
    }
    return classes + interfaces
  }

  private fun annotatedMethods(cls: Class<*>, description: String): List<Method> {
    return try {
      cls.declaredMethods.filter { !it.isBridge && !it.isSynthetic && hasMatchingAnnotation(it, description) }
    } catch (e: LinkageError) {
      logger.debug(e) { "Could not get the methods for class ${cls.name}" }
      emptyList()
    }
  }

  // Annotations are compared by name, as the class being checked could have been loaded by a different class loader
  private fun hasMatchingAnnotation(method: Method, description: String): Boolean {
    return method.declaredAnnotations.any { annotation ->
      if (annotation is PactVerifyProvider) {
        annotation.value == description
      } else {
        val annotationClass = annotation.annotationClass.java
        annotationClass.name == ANNOTATION_NAME &&
          annotationClass.getMethod("value").invoke(annotation) == description
      }
    }
  }

  private fun loadClass(name: String, classLoader: ClassLoader): Class<*>? {
    return try {
      Class.forName(name, false, classLoader)
    } catch (e: ClassNotFoundException) {
      logger.warn { "Could not load class $name: $e" }
      null
    } catch (e: LinkageError) {
      logger.warn { "Could not load class $name: $e" }
      null
    }
  }

  private fun toFile(url: URL): File? {
    if (url.protocol != "file") {
      logger.debug { "Ignoring classpath entry $url, only file URLs are supported" }
      return null
    }
    return runCatching { File(url.toURI()) }.getOrElse { File(url.path) }
  }

  internal fun containsBytes(data: ByteArray, pattern: ByteArray): Boolean {
    return (0..data.size - pattern.size).any { i -> pattern.indices.all { data[i + it] == pattern[it] } }
  }

  internal class PackageMatcher(packages: List<String>) {
    private val patterns = packages.map { it.trim() }
      .filter { it.isNotEmpty() }
      .map { pkg ->
        val base = pkg.removeSuffix(".*")
        val regex = base.split('*').joinToString(".*") { Regex.escape(it) }
        Regex("^$regex(\\..*)?$")
      }

    fun matches(packageName: String) = patterns.isEmpty() || patterns.any { it.matches(packageName) }
  }

  private class Scanner(val matcher: PackageMatcher, val verbose: Boolean) {
    val candidates = LinkedHashSet<String>()
    private val visited = mutableSetOf<File>()

    fun scan(file: File) {
      val canonical = runCatching { file.canonicalFile }.getOrDefault(file.absoluteFile)
      if (!visited.add(canonical)) return

      if (verbose) logger.info { "Scanning classpath entry $canonical" }
      try {
        when {
          canonical.isDirectory -> scanDirectory(canonical)
          canonical.isFile -> scanJar(canonical)
          else -> logger.debug { "Ignoring classpath entry $canonical as it does not exist" }
        }
      } catch (e: IOException) {
        logger.warn { "Failed to scan classpath entry $canonical: $e" }
      }
    }

    private fun scanDirectory(dir: File) {
      dir.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".class") }
        .forEach { file -> checkClass(file.relativeTo(dir).invariantSeparatorsPath) { file.readBytes() } }
    }

    private fun scanJar(file: File) {
      JarFile(file, false).use { jar ->
        jar.entries().asSequence()
          .filter { !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/") }
          .forEach { entry -> checkClass(entry.name) { jar.getInputStream(entry).use { it.readBytes() } } }

        // Follow the Class-Path in the manifest, used by tools that put the classpath in a jar (like Surefire)
        val manifestClasspath = jar.manifest?.mainAttributes?.getValue(Attributes.Name.CLASS_PATH)
        manifestClasspath?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }?.forEach { entry ->
          try {
            toFile(URL(file.toURI().toURL(), entry))?.let { scan(it) }
          } catch (e: MalformedURLException) {
            logger.debug { "Ignoring invalid Class-Path entry '$entry' in $file" }
          }
        }
      }
    }

    private fun checkClass(path: String, bytes: () -> ByteArray) {
      val className = path.removeSuffix(".class").replace('/', '.')
      if (className.endsWith("module-info") || className.endsWith("package-info")) return
      if (!matcher.matches(className.substringBeforeLast('.', ""))) return
      if (containsBytes(bytes(), ANNOTATION_DESCRIPTOR)) {
        if (verbose) logger.info { "Found class $className that references @PactVerifyProvider" }
        candidates.add(className)
      }
    }
  }
}
