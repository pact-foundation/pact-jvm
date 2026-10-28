package au.com.dius.pact.provider

import au.com.dius.pact.provider.methodfinder.FinderChild
import au.com.dius.pact.provider.methodfinder.FinderInterface
import au.com.dius.pact.provider.methodfinder.FinderParent
import au.com.dius.pact.provider.methodfinder.NotAnnotated
import au.com.dius.pact.provider.methodfinder.sub.SubPackageProvider
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class ProviderMethodFinderSpec extends Specification {

  static final String FIXTURE_PACKAGE = 'au.com.dius.pact.provider.methodfinder'
  static final List<Class> ALL_PROVIDERS = [FinderChild, FinderParent, FinderInterface, SubPackageProvider]
  static final List<Class> FIXTURES = ALL_PROVIDERS + [NotAnnotated]

  @TempDir
  Path tempDir

  @Unroll
  def 'findOnClasses finds the annotated method for "#description" on the class hierarchy'() {
    expect:
    ProviderMethodFinder.findOnClasses([FinderChild], description)
      .collect { "${it.declaringClass.simpleName}.${it.name}".toString() } == expected

    where:

    description          | expected
    'child message'      | ['FinderChild.fromChild']
    'parent message'     | ['FinderParent.fromParent']
    'interface message'  | ['FinderInterface.fromInterface']
    'overridden message' | ['FinderChild.overridden']
    'unknown message'    | []
  }

  def 'findOnClasses returns nothing if there are no classes'() {
    expect:
    ProviderMethodFinder.findOnClasses([], 'child message').empty
  }

  def 'scanClasspath finds the classes that have annotated methods in a directory'() {
    given:
    def classesDir = classesLocation()

    expect:
    ProviderMethodFinder.scanClasspath([classesDir], [FIXTURE_PACKAGE]) as Set == [
      FinderChild.name, FinderParent.name, FinderInterface.name, SubPackageProvider.name
    ] as Set
  }

  @Unroll
  def 'scanClasspath only checks the classes in packages matching "#pkg"'() {
    expect:
    ProviderMethodFinder.scanClasspath([classesLocation()], [pkg]) as Set == expected*.name as Set

    where:

    pkg                                                | expected
    'au.com.dius.pact.provider.methodfinder.sub'       | [SubPackageProvider]
    'au.com.dius.pact.provider.methodfinder.*'         | ALL_PROVIDERS
    'au.com.*.methodfinder.sub'                        | [SubPackageProvider]
    'au.com.dius.pact.provider.methodfinde'            | []
    'au.com.dius.pact.provider.methodfinder.sub.other' | []
  }

  def 'scanClasspath finds classes in jar files, and follows the Class-Path in the manifest'() {
    given:
    def fixturesJar = tempDir.resolve('fixtures.jar')
    writeJar(fixturesJar, new Manifest(), FIXTURES)
    def manifest = new Manifest()
    manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, '1.0')
    manifest.mainAttributes.put(Attributes.Name.CLASS_PATH, 'fixtures.jar does-not-exist.jar')
    def pathingJar = tempDir.resolve('pathing.jar')
    writeJar(pathingJar, manifest, [])

    expect:
    ProviderMethodFinder.scanClasspath([pathingJar.toUri().toURL()], [FIXTURE_PACKAGE]) as Set == [
      FinderChild.name, FinderParent.name, FinderInterface.name, SubPackageProvider.name
    ] as Set
  }

  def 'scanClasspath ignores entries that do not exist or are not files'() {
    expect:
    ProviderMethodFinder.scanClasspath([
      tempDir.resolve('missing.jar').toUri().toURL(),
      new URL('http://localhost/classes.jar')
    ], []).empty
  }

  def 'findOnClassNames loads the classes and returns the annotated methods'() {
    expect:
    ProviderMethodFinder.findOnClassNames([SubPackageProvider.name, 'does.not.Exist', NotAnnotated.name],
      getClass().classLoader, 'sub package message')*.name == ['message']
  }

  def 'classpathFor uses the URLs if given'() {
    given:
    def urls = [new URL('file:/classes/')]
    def loader = new URLClassLoader([new URL('file:/other/')] as URL[])

    expect:
    ProviderMethodFinder.classpathFor(loader, urls) == urls
  }

  def 'classpathFor uses the URLs from the class loader'() {
    given:
    def loader = new URLClassLoader([new URL('file:/other/')] as URL[], (ClassLoader) null)

    expect:
    ProviderMethodFinder.classpathFor(loader, []) == [new URL('file:/other/')]
  }

  def 'classpathFor falls back to the JVM classpath'() {
    expect:
    ProviderMethodFinder.classpathFor(null, []).contains(classesLocation())
  }

  private static URL classesLocation() {
    FinderChild.protectionDomain.codeSource.location
  }

  private static void writeJar(Path path, Manifest manifest, List<Class> classes) {
    new JarOutputStream(Files.newOutputStream(path), manifest).withCloseable { jar ->
      classes.each { cls ->
        def name = cls.name.replace('.', '/') + '.class'
        jar.putNextEntry(new JarEntry(name))
        jar.write(cls.classLoader.getResourceAsStream(name).bytes)
        jar.closeEntry()
      }
    }
  }
}
