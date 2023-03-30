package au.com.dius.pact.consumer.dsl;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PactDslJsonBodyTest {

  @Test
  void booleanValueNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      Boolean value = null;
      new PactDslJsonBody()
        .booleanValue("myField", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void stringTypesNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .stringTypes(value);
    });
    assertThat(exception.getMessage(), is("Attribute names can not be null"));
  }

  @Test
  void stringTypeNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .stringType("field", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void numberTypesNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .numberTypes(value);
    });
    assertThat(exception.getMessage(), is("Attribute names can not be null"));
  }

  @Test
  void numberTypeNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      Number value = null;
      new PactDslJsonBody()
        .numberType("field", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void integerTypesNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .integerTypes(value);
    });
    assertThat(exception.getMessage(), is("Attribute names can not be null"));
  }

  @Test
  void integerTypeNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      Integer value = null;
      new PactDslJsonBody()
        .integerType("field", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void decimalTypesNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .decimalTypes(value);
    });
    assertThat(exception.getMessage(), is("Attribute names can not be null"));
  }

  @Test
  void decimalTypeNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      Double value = null;
      new PactDslJsonBody()
        .decimalType("field", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void booleanTypesNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .booleanTypes(value);
    });
    assertThat(exception.getMessage(), is("Attribute names can not be null"));
  }

  @Test
  void booleanTypeNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      Boolean value = null;
      new PactDslJsonBody()
        .booleanType("field", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }

  @Test
  void stringMatcherNullCheck() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      String value = null;
      new PactDslJsonBody()
        .stringMatcher("field", "\\d+", value);
    });
    assertThat(exception.getMessage(), is("Example values can not be null"));
  }
}
