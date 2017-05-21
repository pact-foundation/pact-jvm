package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class MessagePactRunner extends PactRunner {
    public MessagePactRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected List<Pact> filterPacts(List<Pact> pacts) {
        return ListUtils.select(pacts, new Predicate<Pact>() {
            @Override
            public boolean evaluate(Pact pact) {
                return pact.getClass() == MessagePact.class;
            }
        });
    }
}
