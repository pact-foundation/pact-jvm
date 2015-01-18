package au.com.dius.pact.matchers

trait MismatchFactory[Mismatch] {
    def create(expected: Any, actual: Any, message: String, path: String) : Mismatch
}
