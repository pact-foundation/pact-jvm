package au.com.dius.pact.server

import au.com.dius.pact.core.model.IResponse

data class ServerState @JvmOverloads constructor(val state: Map<String, StatefulMockProvider> = mapOf())

data class Result(val response: IResponse, val newState: ServerState)
