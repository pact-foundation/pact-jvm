package com.dius.pact.consumer;

import com.dius.pact.model.Consumer;
import com.dius.pact.model.Interaction;
import com.dius.pact.model.Pact;
import com.dius.pact.model.Provider;

import java.util.ArrayList;
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

    public Pact withInteractions(ConsumerInteractionJavaDsl ... interactions) {
        List<Interaction> builtInteractions = new ArrayList<>();

        return null;//new Pact(new Provider(providerName), new Consumer(consumerName), scalaInteractions);
    }
}
