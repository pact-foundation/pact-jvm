package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.model.RequestResponsePact;
import au.com.dius.pact.model.v3.messaging.Message;
import org.junit.runners.model.InitializationError;

import java.util.List;
import java.util.stream.Collectors;

public class RestPactRunner extends PactRunner {
    public RestPactRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected List<Pact> filterPacts(List<Pact> pacts) {
        return pacts.stream()
                .filter(pact -> pact.getClass() == RequestResponsePact.class)
                .collect(Collectors.toList());
    }
}
