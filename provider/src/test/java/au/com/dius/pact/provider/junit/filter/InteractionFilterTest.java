package au.com.dius.pact.provider.junit.filter;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.ProviderState;
import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.provider.junitsupport.filter.InteractionFilter;
import org.junit.Assert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;

class InteractionFilterTest {

    @Nested
    class ByProviderState {

        InteractionFilter<? super Interaction> interactionFilter =
            InteractionFilter.ByProviderState.class.getDeclaredConstructor().newInstance();

        ByProviderState() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        }

        @Test
        public void filterRequestResponseInteraction() {
            RequestResponseInteraction interaction = new RequestResponseInteraction(
                "test",
                Arrays.asList(new ProviderState("state1"), new ProviderState("state2"))
            );

            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{"noop"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"noop", "state2"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
        }

        @Test
        public void filterMessageInteraction() {
            Message interaction = new Message(
                "test",
                Arrays.asList(new ProviderState("state1"), new ProviderState("state2"))
            );

            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{"noop"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"noop", "state2"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
        }
    }

    @Nested
    class ByRequestPath {

        InteractionFilter<? super Interaction> interactionFilter =
            InteractionFilter.ByRequestPath.class.getDeclaredConstructor().newInstance();

        ByRequestPath() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        }

        @Test
        public void filterRequestResponseInteraction() {
            RequestResponseInteraction interaction = new RequestResponseInteraction(
                "test",
                Collections.emptyList(),
                new Request("GET", "/some-path")
            );

            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{"other"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path.*"}).test(interaction));
            Assert.assertTrue(interactionFilter.buildPredicate(new String[]{".*some-path"}).test(interaction));
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
        }

        @Test
        public void filterMessageInteraction() {
            Message interaction = new Message("test", Collections.emptyList());
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{".*"}).test(interaction));
        }
    }
}
