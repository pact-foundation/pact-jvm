package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;

public interface TestRun {
    void run(MockProviderConfig config) throws Throwable;
}
