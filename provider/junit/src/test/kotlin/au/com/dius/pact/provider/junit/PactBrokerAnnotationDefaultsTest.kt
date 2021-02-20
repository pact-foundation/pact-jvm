package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseListExpression
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.collection.IsArrayWithSize.arrayWithSize
import org.junit.Before
import org.junit.Test
import java.util.Properties

class PactBrokerAnnotationDefaultsTest {

    val annotation: PactBroker = SampleBrokerClass::class.java.getAnnotation(PactBroker::class.java)

    val props: Properties = System.getProperties()

    @Before
    fun setUp() {
        clearPactBrokerProperties()
    }

    fun clearPactBrokerProperties() =
            props.keys
                    .filter { it is String }
                    .map { it as String }
                    .filter { it.startsWith("pactbroker") }
                    .forEach { props.remove(it) }

    @Test
    fun `default url is empty`() {
      assertThat(parseExpression(annotation.url, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set url`() {
      props.setProperty("pactbroker.url", "http://myHost")
      assertThat(parseExpression(annotation.url, DataType.RAW)?.toString(), `is`("http://myHost"))
    }

    @Test
    fun `default host is empty`() {
        assertThat(parseExpression(annotation.host, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set host`() {
        props.setProperty("pactbroker.host", "myHost")
        assertThat(parseExpression(annotation.host, DataType.RAW)?.toString(), `is`("myHost"))
    }

    @Test
    fun `default port is empty`() {
        assertThat(parseExpression(annotation.port, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set port`() {
        props.setProperty("pactbroker.port", "myPort")
        assertThat(parseExpression(annotation.port, DataType.RAW)?.toString(), `is`("myPort"))
    }

    @Test
    fun `default protocol is http`() {
        assertThat(parseExpression(annotation.scheme, DataType.RAW)?.toString(), `is`("http"))
    }

    @Test
    fun `can set scheme`() {
        props.setProperty("pactbroker.scheme", "myProtocol")
        assertThat(parseExpression(annotation.scheme, DataType.RAW)?.toString(), `is`("myProtocol"))
    }

    @Test
    fun `default tag is empty`() {
        assertThat(annotation.tags, arrayWithSize(1))
        assertThat(parseListExpression(annotation.tags[0]), empty())
    }

    @Test
    fun `can set single tags`() {
        props.setProperty("pactbroker.tags", "myTag")
        assertThat(parseListExpression(annotation.tags[0]), contains("myTag"))
    }

    @Test
    fun `can set multiple tags`() {
        props.setProperty("pactbroker.tags", "myTag1,myTag2")
        assertThat(parseListExpression(annotation.tags[0]), contains("myTag1", "myTag2"))
    }

    @Test
    fun `default consumer filter is empty (all consumers)`() {
        assertThat(annotation.consumers, arrayWithSize(1))
        assertThat(parseListExpression(annotation.consumers[0]), empty())
    }

    @Test
    fun `can set single consumer`() {
        props.setProperty("pactbroker.consumers", "myConsumer")
        assertThat(parseListExpression(annotation.consumers[0]), contains("myConsumer"))
    }

    @Test
    fun `can set multiple consumers`() {
        props.setProperty("pactbroker.consumers", "myConsumer1,myConsumer2")
        assertThat(parseListExpression(annotation.consumers[0]), contains("myConsumer1", "myConsumer2"))
    }

    @Test
    fun `default auth username is empty`() {
        assertThat(parseExpression(annotation.authentication.username, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set auth username`() {
        props.setProperty("pactbroker.auth.username", "myUser")
        assertThat(parseListExpression(annotation.authentication.username), contains("myUser"))
    }

    @Test
    fun `default auth password is empty`() {
        assertThat(parseExpression(annotation.authentication.password, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set auth password`() {
        props.setProperty("pactbroker.auth.password", "myPass")
        assertThat(parseListExpression(annotation.authentication.password), contains("myPass"))
    }

    @Test
    fun `default auth token is empty`() {
        assertThat(parseExpression(annotation.authentication.token, DataType.RAW)?.toString(), `is`(""))
    }

    @Test
    fun `can set auth token`() {
        props.setProperty("pactbroker.auth.token", "myToken")
        assertThat(parseListExpression(annotation.authentication.token), contains("myToken"))
    }

    @PactBroker
    class SampleBrokerClass
}
