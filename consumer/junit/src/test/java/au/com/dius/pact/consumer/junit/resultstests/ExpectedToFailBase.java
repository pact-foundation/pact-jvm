package au.com.dius.pact.consumer.junit.resultstests;

import au.com.dius.pact.consumer.junit.ConsumerPactTest;

import static org.junit.Assert.fail;

public abstract class ExpectedToFailBase extends ConsumerPactTest {

    private final Class expectedException;

    public ExpectedToFailBase(Class expectedException) {
        this.expectedException = expectedException;
    }

    @Override
    public void testPact() {
        try {
            super.testPact();
            fail("Expected an exception of type " + expectedException.getName());
        } catch (Throwable e) {
            if (!expectedException.isAssignableFrom(e.getClass())) {
                throw new AssertionError(e);
            }
            assertException(e);
        }
    }

    protected abstract void assertException(Throwable e);
}
