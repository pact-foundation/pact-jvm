package au.com.dius.pact.consumer

import au.com.dius.pact.core.support.BuiltToolConfig

data class PactTestExecutionContext(var pactFolder: String = BuiltToolConfig.pactDirectory)
