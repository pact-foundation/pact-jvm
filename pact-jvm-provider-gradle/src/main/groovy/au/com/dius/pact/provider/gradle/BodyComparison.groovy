package au.com.dius.pact.provider.gradle

class BodyComparison {

    static compare(String path, def expected, def actual) {
        def result = [:]
        if (expected != actual) {
            if ((expected instanceof Map && !(actual instanceof Map)) || (expected instanceof List && !(actual instanceof List))) {
                result[path] = "Type mismatch: Expected ${typeOf(expected)} ${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}"
            } else if (expected instanceof Map) {
                if (expected.size() == 0 && actual.size() != 0) {
                    result[path] = "Expected an empty Map but received ${valueOf(actual)}"
                } else {
                    if (expected.size() > actual.size()) {
                        result[path] = "Expected a Map with atleast ${expected.size()} elements but received ${actual.size()} elements"
                    }
                    expected.each { key, value ->
                        def s = path + key + '/'
                        if (actual.containsKey(key)) {
                            result << compare(s, value, actual[key])
                        } else {
                            result[s] = "Expected ${valueOf(value)} but was missing"
                        }
                    }
                }
            } else if (expected instanceof List) {
                if (expected.size() == 0 && actual.size() != 0) {
                    result[path] = "Expected an empty List but received ${valueOf(actual)}"
                } else {
                    if (expected.size() != actual.size()) {
                        result[path] = "Expected a List with ${expected.size()} elements but received ${actual.size()} elements"
                    }
                    expected.eachWithIndex { value, index ->
                        def s = path + index + '/'
                        if (index < actual.size()) {
                            result << compare(s, value, actual[index])
                        } else {
                            result[s] = "Expected ${valueOf(value)} but was missing"
                        }
                    }
                }
            } else {
                result[path] = "Expected ${valueOf(expected)} but received ${valueOf(actual)}"
            }
        }
        result
    }

    static String typeOf(def p) {
        if (p == null) {
            'Null'
        } else if (p instanceof Map) {
            'Map'
        } else if (p instanceof List) {
            'List'
        } else {
            p.class.simpleName
        }
    }

    static def valueOf(def p) {
        if (p instanceof String) {
            "'${p}'"
        } else {
            p
        }
    }
}
