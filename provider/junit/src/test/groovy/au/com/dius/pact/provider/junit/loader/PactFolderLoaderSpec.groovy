package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader
import kotlin.jvm.JvmClassMappingKt
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class PactFolderLoaderSpec extends Specification {

  def 'handles the case where the configured directory does not exist'() {
    given:
    def file = new File('/does/not/exist')

    when:
    def result = new PactFolderLoader(file).load('provider')

    then:
    result == []
  }

  def 'only includes json files'() {
    given:
    PactFolder annotation = ParticularFolderPactLoaderAnnotation.class.getAnnotation(PactFolder)

    when:
    def result = new PactFolderLoader(annotation).load('myAwesomeService')

    then:
    result.size() == 3
  }

  def 'only includes json files that match the provider name'() {
    given:
    PactFolder annotation = ParticularFolderPactLoaderAnnotation.class.getAnnotation(PactFolder)

    when:
    def result = new PactFolderLoader(annotation).load('myAwesomeService2')

    then:
    result.size() == 1
  }

  def 'is able to load files from a directory'() {
    given:
    File tmpDir = File.createTempDir()
    tmpDir.deleteOnExit()
    File pactFile = new File(tmpDir, 'pact.json')
    pactFile.deleteOnExit()
    pactFile.text = this.class.classLoader.getResourceAsStream('pacts/contract.json').text

    when:
    def result = new PactFolderLoader(tmpDir.path).load('myAwesomeService')

    then:
    result.size() == 1
  }

  def 'is able to load files from a directory with spaces in the path'() {
    given:
    def dirWithSpaces = 'dir with spaces!'

    when:
    def result = new PactFolderLoader(dirWithSpaces).load('myAwesomeService')

    then:
    result.size() == 1
  }

  @RestoreSystemProperties
  def "resolves path using default resolver (SystemPropertyResolver)"() {
    given:
    def exprPath = 'pact${valueToBeResolved}'
    System.setProperty('valueToBeResolved', "s")

    when:
    def result = new PactFolderLoader(exprPath).load('myAwesomeService')

    then:
    result.size() == 3
  }

  def "resolves path using given resolver"() {
    given:
    def exprPath = 'pact${valueToBeResolved}'
    def valueResolver = [resolveValue: { val -> 's' }] as ValueResolver

    when:
    def result = new PactFolderLoader(exprPath, null, valueResolver).load('myAwesomeService')

    then:
    result.size() == 3
  }

  def "resolves path using given resolver class"() {
    given:
    def exprPath = 'pact${valueToBeResolved}'
    def constantValueResolver = JvmClassMappingKt.getKotlinClass(ConstantValueResolver.class)

    when:
    def result = new PactFolderLoader(exprPath, constantValueResolver).load('myAwesomeService')

    then:
    result.size() == 3
  }

  @RestoreSystemProperties
  def "resolves path using minimal annotation (resolver SystemPropertyResolver)"() {
    given:
    System.setProperty('pactfolder.path', "pacts")
    def annotation = MinimalPactLoaderAnnotation.class.getAnnotation(PactFolder.class)

    when:
    def result = new PactFolderLoader(annotation).load('myAwesomeService')

    then:
    result.size() == 3
  }

  @RestoreSystemProperties
  def "resolves path using given revolver class via annotation"() {
    given:
    System.setProperty('pactfolder.path', "pacts")
    def annotation = ParticularResolverPactLoaderAnnotation.class.getAnnotation(PactFolder.class)

    when:
    def result = new PactFolderLoader(annotation).load('myAwesomeService')

    then:
    result.size() == 3
  }

  @PactFolder
  static class MinimalPactLoaderAnnotation {

  }

  @PactFolder('pacts')
  static class ParticularFolderPactLoaderAnnotation {

  }

  @PactFolder(value = 'pact${valueToBeResolved}', valueResolver = ConstantValueResolver)
  static class ParticularResolverPactLoaderAnnotation {

  }

  static class ConstantValueResolver implements ValueResolver {

    @Override
    String resolveValue(@Nullable String property) {
      return 's'
    }

    @Override
    String resolveValue(@Nullable String property, @Nullable String s) {
      return 's'
    }

    @Override
    boolean propertyDefined(@NotNull String property) {
      return true
    }
  }

}
