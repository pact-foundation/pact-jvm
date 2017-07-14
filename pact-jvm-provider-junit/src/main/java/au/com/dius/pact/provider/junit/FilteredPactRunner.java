package au.com.dius.pact.provider.junit;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Optional<PactFilter> pactFilterOpt = Optional.ofNullable(this.getTestClass().getJavaClass().getAnnotation(PactFilter.class));

        return pactFilterOpt.map(pactFilter -> {
            Set<String> requiredInteractions = Sets.newHashSet(pactFilter.value());

            if (requiredInteractions != null && requiredInteractions.size() > 0) {
                pacts.forEach(pact ->
                        pact.getInteractions().removeIf(interaction -> !requiredInteractions.contains(interaction.getProviderState())));
            }
            return pacts;
        }).orElse(pacts);
    }
}
