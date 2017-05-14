package au.com.dius.pact.provider.junit.sysprops

import org.junit.Test

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerTagListExpressionParser.parseTagListExpressions
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat

@SuppressWarnings('GStringExpressionWithinString')
class PactRunnerTagListExpressionParserTest {

    private final Map mappings = [
            '${empty}': '',
            '${single}': 'one',
            '${double}': 'one,two'
    ]

    private final ValueResolver valueResolver = [
            resolveValue: { expression -> mappings[expression] }
    ] as ValueResolver

    @Test
    void 'Does not modify Strings with no expressions'() {
        assertThat(parseTagListExpressions([]), is(equalTo([])))
        assertThat(parseTagListExpressions(['hello']), is(equalTo(['hello'])))
        assertThat(parseTagListExpressions(['one', 'weird$', '']), is(equalTo(['one', 'weird$', ''])))
    }

    @Test(expected = RuntimeException)
    void 'Throws An Exception On Unterminated Expressions'() {
        parseTagListExpressions(['${value'])
    }

    @Test
    void 'omits empty tags'() {
        assertThat(parseTagListExpressions(['${empty}'], valueResolver), is(equalTo([])))
        assertThat(parseTagListExpressions(['a', '${empty}', 'b'], valueResolver), is(equalTo(['a', 'b'])))
    }

    @Test
    void 'converts single values'() {
        assertThat(parseTagListExpressions(['${single}'], valueResolver), is(equalTo(['one'])))
        assertThat(parseTagListExpressions(['a', '${single}', 'b'], valueResolver), is(equalTo(['a', 'one', 'b'])))
    }

    @Test
    void 'converts double values'() {
        assertThat(parseTagListExpressions(['${double}'], valueResolver), is(equalTo(['one', 'two'])))
        assertThat(parseTagListExpressions(['a', '${double}', 'b'], valueResolver),
                is(equalTo(['a', 'one', 'two', 'b'])))
        assertThat(parseTagListExpressions(['${double}', '${double}'], valueResolver),
                is(equalTo(['one', 'two', 'one', 'two'])))
    }

    @Test
    void 'converts mixed values'() {
        assertThat(parseTagListExpressions(['${single}', '${empty}', '${double}'], valueResolver),
                is(equalTo(['one', 'one', 'two'])))
    }
}
