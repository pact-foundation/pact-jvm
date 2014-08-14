package au.com.dius.pact.provider.gradle

import org.junit.Test

class BodyComparisonTest {

    @Test
    void 'handles basic types correctly'() {
        assert BodyComparison.compare('/', 'test', 'test') == [:]
        assert BodyComparison.compare('/', true, true) == [:]
        assert BodyComparison.compare('/', false, false) == [:]
        assert BodyComparison.compare('/', null, null) == [:]
        assert BodyComparison.compare('/', 100, 100) == [:]

        assert BodyComparison.compare('/', 'test', 'test2') == ['/': "Expected 'test' but received 'test2'"]
        assert BodyComparison.compare('/', true, false) == ['/': "Expected true but received false"]
        assert BodyComparison.compare('/', false, 100) == ['/': "Expected false but received 100"]
        assert BodyComparison.compare('/', 100, null) == ['/': "Expected 100 but received null"]
        assert BodyComparison.compare('/', 101, 100) == ['/': "Expected 101 but received 100"]
    }

    @Test
    void 'handles mismatched collections'() {
        assert BodyComparison.compare('/', [:], 'test') == ['/': "Type mismatch: Expected Map [:] but received String 'test'"]
        assert BodyComparison.compare('/', [], false) == ['/': "Type mismatch: Expected List [] but received Boolean false"]
        assert BodyComparison.compare('/', [:], []) == ['/': "Type mismatch: Expected Map [:] but received List []"]
        assert BodyComparison.compare('/', [], [:]) == ['/': "Type mismatch: Expected List [] but received Map [:]"]
        assert BodyComparison.compare('/', [100], 100) == ['/': "Type mismatch: Expected List [100] but received Integer 100"]
    }

    @Test
    void 'handles maps correctly'() {
        assert BodyComparison.compare('/', [a: 100, b:100], [a: 100, b:100]) == [:]
        assert BodyComparison.compare('/', [b: 100], [b:100, a: 101]) == [:]

        assert BodyComparison.compare('/', [c: 100], [b:100, a: 101]) == ['/c/': 'Expected 100 but was missing']
        assert BodyComparison.compare('/', [c: 100], [c:200]) == ['/c/': 'Expected 100 but received 200']
        assert BodyComparison.compare('/', [:], [c:200]) == ['/': 'Expected an empty Map but received [c:200]']
    }

    @Test
    void 'handles lists correctly'() {
        assert BodyComparison.compare('/', [100, 100], [100, 100]) == [:]

        assert BodyComparison.compare('/', [100, 200], [200, 100]) == [
                '/0/': 'Expected 100 but received 200',
                '/1/': 'Expected 200 but received 100']
        assert BodyComparison.compare('/', [100], [101, 102]) == [
                '/': 'Expected a List with 1 elements but received 2 elements',
                '/0/': 'Expected 100 but received 101'
        ]
        assert BodyComparison.compare('/', [100, 101], [100]) == [
                '/': 'Expected a List with 2 elements but received 1 elements',
                '/1/': 'Expected 101 but was missing'
        ]
        assert BodyComparison.compare('/', [], [100]) == ['/': 'Expected an empty List but received [100]']
    }
}
