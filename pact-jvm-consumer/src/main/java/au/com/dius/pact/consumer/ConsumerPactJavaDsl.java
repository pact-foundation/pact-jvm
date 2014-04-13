package au.com.dius.pact.consumer;

import au.com.dius.pact.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsumerPactJavaDsl {

    public ConsumerPactJavaDsl() {

    }

    public static ConsumerPactJavaDsl makePact() {
        return new ConsumerPactJavaDsl();
    }

    private String providerName;
    public ConsumerPactJavaDsl withProvider(String name) {
        providerName = name;
        return this;
    }

    private String consumerName;
    public ConsumerPactJavaDsl withConsumer(String name) {
        consumerName = name;
        return this;
    }

    public Pact withInteractions(Interaction ... interactions) {
        return Pact$.MODULE$.apply(new Provider(providerName), new Consumer(consumerName), Arrays.asList(interactions));
    }
}
