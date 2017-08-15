package au.com.dius.pact.provider.junit;

import java.util.List;
import java.util.Set;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.loader.PactFilter;

import org.junit.runners.model.InitializationError;

import com.google.common.collect.Sets;

public class FilteredPactRunner extends PactRunner {

    public FilteredPactRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public List<Pact> filterPacts(List<Pact> pacts){
        PactFilter pactFilterOpt = this.getTestClass().getJavaClass().getAnnotation(PactFilter.class);

        if (pactFilterOpt == null) {
            return pacts;
        }

        Set<String> requiredInteractions = Sets.newHashSet(pactFilterOpt.value());
        if (requiredInteractions.size() > 0) {
            for (Pact pact: pacts) {
                for (Interaction interaction: pact.getInteractions()) {
                    if (!requiredInteractions.contains(interaction.getProviderState())) {
                        pact.getInteractions().remove(interaction);
                    }
                }
            }
        }
        return pacts;
    }
}
