package au.com.dius.pact.consumer

import groovy.json.JsonSlurper
import org.junit.Before
import org.junit.Test

class PactDslJsonArrayMatcherTest {

    private PactDslJsonArray subject

    @Before
    void setup() {
        subject = new PactDslJsonArray()
    }

    @Test(expected = InvalidMatcherException)
    void stringMatcherThrowsExceptionIfTheExampleDoesNotMatchThePattern() {
        subject.stringMatcher('[a-z]+', 'dfhdsjf87fdjh')
    }

    @Test(expected = InvalidMatcherException)
    void hexMatcherThrowsExceptionIfTheExampleIsNotAHexadecimalValue() {
        subject.hexValue('dfhdsjf87fdjh')
    }

    @Test(expected = InvalidMatcherException)
    void uuidMatcherThrowsExceptionIfTheExampleIsNotAUuid() {
        subject.uuid('dfhdsjf87fdjh')
    }

    @Test
    void allowsLikeMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date()
        subject = (PactDslJsonArray) PactDslJsonArray.arrayEachLike()
            .date('clearedDate', 'mm/dd/yyyy', date)
            .stringType('status', 'STATUS')
            .realType('amount', 100.0)
            .closeObject()

        assert new JsonSlurper().parseText(subject.body.toString()) == [
          [amount: 100, clearedDate: date.format('mm/dd/yyyy'), status: 'STATUS']
        ]
        assert subject.matchers.keySet() == ['', '[*].amount', '[*].clearedDate', '[*].status'] as Set
    }

    @Test
    void allowsLikeMinMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date()
        subject = (PactDslJsonArray) PactDslJsonArray.arrayMinLike(1)
            .date('clearedDate', 'mm/dd/yyyy', date)
            .stringType('status', 'STATUS')
            .realType('amount', 100.0)
            .closeObject()

        assert new JsonSlurper().parseText(subject.body.toString()) == [
          [amount: 100, clearedDate: date.format('mm/dd/yyyy'), status: 'STATUS']
        ]
        assert subject.matchers.keySet() == ['', '[*].amount', '[*].clearedDate', '[*].status'] as Set
    }

    @Test
    void allowsLikeMaxMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date()
        subject = (PactDslJsonArray) PactDslJsonArray.arrayMaxLike(10)
            .date('clearedDate', 'mm/dd/yyyy', date)
            .stringType('status', 'STATUS')
            .realType('amount', 100.0)
            .closeObject()

        assert new JsonSlurper().parseText(subject.body.toString()) == [
          [amount: 100, clearedDate: date.format('mm/dd/yyyy'), status: 'STATUS']
        ]
        assert subject.matchers.keySet() == ['', '[*].amount', '[*].clearedDate', '[*].status'] as Set
    }
}
