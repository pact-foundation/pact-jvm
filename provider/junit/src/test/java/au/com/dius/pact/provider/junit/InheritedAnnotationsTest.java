package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.apache.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InheritedAnnotationsTest {

    @Test
    public void shouldHaveInheritedAnnotations() {
        SampleProviderTest clazz = new SampleProviderTest();
        List<? extends Class<? extends Annotation>> list = Arrays.stream(clazz.getClass().getAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toList());

        Assert.assertTrue(list.containsAll(
                Arrays.asList(
                        PactBroker.class,
                        Provider.class,
                        Consumer.class,
                        PactFolder.class,
                        IgnoreNoPactsToVerify.class,
                        PactFilter.class)));
    }

    private class SampleProviderTest extends ParentClazz {
        @State("has no data")
        public void hasNoData() {
            System.out.println("Has no data state");
        }

        @TargetRequestFilter
        public void requestFilter(HttpRequest  httpRequest) {

        }
    }

    @PactBroker
    @Provider("testProvider")
    @Consumer("testConsumer")
    @PactFolder("pactFolder")
    @IgnoreNoPactsToVerify
    @PactFilter("myFilter")
    abstract class ParentClazz {

    }
}
