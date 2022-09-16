package au.com.dius.pact.provider.spring.junit5;

import org.junit.jupiter.api.Test;

import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerLoader;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PactBrokerLoaderTest {

    @Test
    void test1() {
        assertNotNull(PactBrokerLoader.testClassHasSelectorsMethod(Test1.class));
    }

    @Test
    void test2() {
        assertThrows(IllegalAccessException.class, () -> PactBrokerLoader.testClassHasSelectorsMethod(Test2.class));
    }

    @Test
    void test3() {
        assertThrows(IllegalAccessException.class, () -> PactBrokerLoader.testClassHasSelectorsMethod(Test3.class));
    }

    @Test
    void test4() {
        assertThrows(IllegalAccessException.class, () -> PactBrokerLoader.testClassHasSelectorsMethod(Test4.class));
    }

    @Test
    void test5() {
        assertNotNull(PactBrokerLoader.testClassHasSelectorsMethod(Test5.class));
    }

    class Test1 {
        @PactBrokerConsumerVersionSelectors
        public static SelectorBuilder cvs() {
            return new SelectorBuilder();
        }
    }
    class Test2 {
        @PactBrokerConsumerVersionSelectors
        static SelectorBuilder cvs() {
            return new SelectorBuilder();
        }
    }

    class Test3 {
        @PactBrokerConsumerVersionSelectors
        private static SelectorBuilder cvs() {
            return new SelectorBuilder();
        }
    }

    class Test4 extends Test4Super {}

    class Test4Super {
        @PactBrokerConsumerVersionSelectors
        protected static SelectorBuilder cvs() {
            return new SelectorBuilder();
        }
    }

    class Test5 extends Test5Super {}

    class Test5Super {
        @PactBrokerConsumerVersionSelectors
        public static SelectorBuilder cvs() {
            return new SelectorBuilder();
        }
    }
}
