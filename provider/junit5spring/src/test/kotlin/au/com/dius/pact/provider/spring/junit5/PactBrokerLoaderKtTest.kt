package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerLoader.Companion.testClassHasSelectorsMethod
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PactBrokerLoaderKtTest {

    @Test
    fun test1() {
        assertNotNull(testClassHasSelectorsMethod(Test1::class.java))
    }

    @Test
    fun test2() {
        assertThrows(IllegalAccessException::class.java) {
            testClassHasSelectorsMethod(Test2::class.java)
        }
    }

    @Test
    fun test3() {
        assertThrows(IllegalAccessException::class.java) {
            testClassHasSelectorsMethod(Test3::class.java)
        }
    }

    @Test
    fun test4() {
        assertThrows(IllegalAccessException::class.java) {
            testClassHasSelectorsMethod(Test4::class.java)
        }
    }

    @Test
    fun test5() {
        assertNotNull(testClassHasSelectorsMethod(Test5::class.java))
    }

    @Test
    fun test6() {
        assertNotNull(testClassHasSelectorsMethod(Test6::class.java))
    }

    class Test1 {
        @PactBrokerConsumerVersionSelectors
        fun cvs(): SelectorBuilder {
            return SelectorBuilder()
        }
    }

    class Test2 {
        @PactBrokerConsumerVersionSelectors
        private fun cvs(): SelectorBuilder {
            return SelectorBuilder()
        }
    }

    class Test3 {
        @PactBrokerConsumerVersionSelectors
        private fun cvs(): SelectorBuilder {
            return SelectorBuilder()
        }
    }

    class Test4 : Test4Super()

    abstract class Test4Super {

        @PactBrokerConsumerVersionSelectors
        protected fun cvs(): SelectorBuilder {
            return SelectorBuilder()
        }
    }

    class Test5 : Test5Super()

    abstract class Test5Super {
        @PactBrokerConsumerVersionSelectors
        fun cvs(): SelectorBuilder {
            return SelectorBuilder()
        }
    }

    class Test6 : Test6Super()

    abstract class Test6Super() {
        companion object {
            @PactBrokerConsumerVersionSelectors
            fun cvs(): SelectorBuilder {
                return SelectorBuilder()
            }
        }
    }
}