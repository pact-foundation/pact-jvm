package au.com.dius.pact.provider.junit.filter;

import au.com.dius.pact.core.model.HttpRequest;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.ProviderState;
import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.provider.junitsupport.filter.InteractionFilter;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class InteractionFilterTest {

    @Nested
    class ByProviderState {

        InteractionFilter<? super Interaction> interactionFilter =
            InteractionFilter.ByProviderState.class.getDeclaredConstructor().newInstance();

        ByProviderState() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        }

        @ParameterizedTest
        @ValueSource(classes = {
          RequestResponseInteraction.class,
          Message.class,
          V4Interaction.SynchronousHttp.class,
          V4Interaction.SynchronousMessages.class,
          V4Interaction.AsynchronousMessage.class
        })
        public void filterInteraction(Class interactionClass)
          throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            Interaction interaction = (Interaction) interactionClass.getDeclaredConstructor(String.class, List.class).newInstance(
                "test",
                Arrays.asList(new ProviderState("state1"), new ProviderState("state2"))
            );

            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"state1"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{"noop"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"noop", "state2"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"state1", "state2"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
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

            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{"other"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path.*"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{".*some-path"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
        }

        @Test
        public void filterSynchronousHttpInteraction() {
            V4Interaction.SynchronousHttp interaction = new V4Interaction.SynchronousHttp(
              "key",
              "test",
              Collections.emptyList(),
              new HttpRequest("GET", "/some-path")
            );

            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{"other"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{"\\/some-path.*"}).test(interaction));
            Assertions.assertTrue(interactionFilter.buildPredicate(new String[]{".*some-path"}).test(interaction));
            Assertions.assertFalse(interactionFilter.buildPredicate(new String[]{""}).test(interaction));
        }

        @ParameterizedTest
        @ValueSource(classes = {
          Message.class,
          V4Interaction.SynchronousMessages.class,
          V4Interaction.AsynchronousMessage.class
        })
        public void filterMessageInteraction(Class interactionClass)
          throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            Interaction interaction = (Interaction) interactionClass.getDeclaredConstructor(String.class, List.class).newInstance(
              "test",
              Collections.emptyList()
            );
            Assert.assertFalse(interactionFilter.buildPredicate(new String[]{".*"}).test(interaction));
        }
    }
}
