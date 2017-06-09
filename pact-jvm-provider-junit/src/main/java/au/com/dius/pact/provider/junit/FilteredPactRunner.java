package com.cisco.lockhart.pact;

import java.util.List;
import java.util.Set;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.PactRunner;

import org.junit.runners.model.InitializationError;

import com.google.common.collect.Sets;

/*
 * Copyright (c) Cisco Systems 2017. All rights reserved.
 *
 */

public class FilteredPactRunner extends PactRunner {

    public FilteredPactRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public List<Pact> filterPacts(List<Pact> pacts){
        PactFilter pactFilter = this.getTestClass().getJavaClass().getAnnotation(PactFilter.class);
        Set<String> requiredInteractions = Sets.newHashSet(pactFilter.value());

        if (requiredInteractions != null && requiredInteractions.size() > 0) {
            pacts.forEach(pact ->
                    pact.getInteractions().removeIf(interaction -> !requiredInteractions.contains(interaction.getProviderState())));
        }
        return pacts;
    }
}
