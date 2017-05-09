package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.RequestResponsePact;
import au.com.dius.pact.model.v3.messaging.Message;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.runners.model.InitializationError;

import java.util.List;
import java.util.stream.Collectors;

public class MessagePactRunner extends PactRunner {
    public MessagePactRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected List<Pact> filterPacts(List<Pact> pacts) {
        return pacts.stream()
                .filter(pact -> pact.getClass() == MessagePact.class)
                .collect(Collectors.toList());
    }
}
