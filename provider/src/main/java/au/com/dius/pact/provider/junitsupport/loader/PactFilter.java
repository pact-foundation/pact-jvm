package au.com.dius.pact.provider.junitsupport.loader;

import au.com.dius.pact.provider.junitsupport.filter.InteractionFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to filter pacts. The default implementation is to filter by provider state.
 * The filter supports regular expressions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PactFilter {

    /**
     * Values to use for filtering. Regular expressions are allowed, like "^state \\d".
     * If none of the provided values matches, the interaction is not verified.
     */
    String[] value();

    /**
     * Use this class as filter implementation. The class must implement the {@link InteractionFilter}
     * interface and provide a default constructor.
     *
     * The default value is filtering by provider state.
     */
    Class<? extends InteractionFilter> filter() default InteractionFilter.ByProviderState.class;
}
