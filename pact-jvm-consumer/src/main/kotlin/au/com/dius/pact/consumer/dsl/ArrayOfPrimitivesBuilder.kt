package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import com.mifmif.common.regex.Generex
import org.json.JSONArray

class ArrayOfPrimitivesBuilder {

  var minLength: Int? = null
  var maxLength: Int? = null
  var examples: Int = 1
  var matcher: MatchingRule? = null
  var value: Any? = null
  var generator: Generator? = null

  /**
   * Array must have a minimum length
   * @param min Minimum length
   */
  fun withMinLength(min: Int): ArrayOfPrimitivesBuilder {
    this.minLength = min
    return this
  }

  /**
   * Array must have a maximum length
   * @param max Maximum length
   */
  fun withMaxLength(max: Int): ArrayOfPrimitivesBuilder {
    this.maxLength = max
    return this
  }

  /**
   * Sets the number of examples to generate in the array
   * @param examples Number of examples to generate. It must fall within in min and max bounds that are set
   */
  fun withNumberOfExamples(examples: Int): ArrayOfPrimitivesBuilder {
    if (minLength != null && examples < minLength!!) {
      throw IllegalArgumentException("Number of example $examples is less than the minimum size of $minLength")
    }
    if (this.maxLength != null && examples > this.maxLength!!) {
      throw IllegalArgumentException("Number of example $examples is more than the maximum size of $maxLength")
    }
    this.examples = examples
    return this
  }

  /**
   * All the values in the array must match the provided regex
   * @param regex Regex to match
   */
  fun thatMatchRegex(regex: String): ArrayOfPrimitivesBuilder {
    this.matcher = RegexMatcher(regex)
    this.generator = RegexGenerator(regex)
    this.value = Generex(regex).random()
    return this
  }

  /**
   * All the values in the array must match the provided regex
   * @param regex Regex to match
   * @param example Example value to use when generating bodies
   */
  fun thatMatchRegex(regex: String, example: String): ArrayOfPrimitivesBuilder {
    if (!example.matches(regex.toRegex())) {
      throw InvalidMatcherException("example \"$value\" does not match regular expression \"$regex\"")
    }
    this.matcher = RegexMatcher(regex, example)
    this.value = example
    return this
  }

  /**
   * Consumes this builder and returns the DslPart that it represents
   */
  fun build(): DslPart {
    val array = PactDslJsonArray("", "", null, true)
    array.numberExamples = this.examples
    if (this.minLength != null && this.maxLength != null) {
      array.getMatchers().addRule("", MinMaxTypeMatcher(this.minLength!!, this.maxLength!!))
    } else if (this.minLength != null) {
      array.getMatchers().addRule("", MinTypeMatcher(this.minLength!!))
    } else if (this.maxLength != null) {
      array.getMatchers().addRule("", MaxTypeMatcher(this.maxLength!!))
    }

    if (matcher != null) {
      array.getMatchers().addRule("[*]", matcher!!)
    }
    if (generator != null) {
      array.getGenerators().addGenerator(Category.BODY, "[*]", generator!!)
    }
    for (i in 0 until this.examples) {
      (array.body as JSONArray).put(this.value)
    }

    return array
  }
}
