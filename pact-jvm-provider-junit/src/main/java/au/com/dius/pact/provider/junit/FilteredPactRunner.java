package au.com.dius.pact.provider.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.loader.PactFilter;

import au.com.dius.pact.util.Optional;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.junit.runners.model.InitializationError;

import com.google.common.collect.Sets;

public class FilteredPactRunner extends PactRunner {

    public FilteredPactRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public List<Pact> filterPacts(List<Pact> pacts){
        Optional<PactFilter> pactFilterOpt = Optional.ofNullable(this.getTestClass().getJavaClass().getAnnotation(PactFilter.class));
        if (pactFilterOpt.isPresent()) {
          final Set<String> requiredInteractions = Sets.newHashSet(pactFilterOpt.get().value());
          if (!requiredInteractions.isEmpty()) {
            for (Pact pact: pacts) {
              List<Interaction> interactions = new ArrayList<>(pact.getInteractions());
              for (Interaction interaction: interactions) {
                if (!requiredInteractions.contains(interaction.getProviderState())) {
                  pact.getInteractions().remove(interaction);
                }
              }
            }
          }
        }
        return pacts;
    }
}
