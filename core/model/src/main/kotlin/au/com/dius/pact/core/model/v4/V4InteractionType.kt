package au.com.dius.pact.core.model.v4

import au.com.dius.pact.core.support.Result

enum class V4InteractionType {
  SynchronousHTTP,
  AsynchronousMessages,
  SynchronousMessages;

  override fun toString(): String {
    return when (this) {
      SynchronousHTTP -> "Synchronous/HTTP"
      AsynchronousMessages -> "Asynchronous/Messages"
      SynchronousMessages -> "Synchronous/Messages"
    }
  }

  companion object {
    fun fromString(str: String): Result<V4InteractionType, String> {
      return when (str) {
        "Synchronous/HTTP" -> Result.Ok(SynchronousHTTP)
        "Asynchronous/Messages" -> Result.Ok(AsynchronousMessages)
        "Synchronous/Messages" -> Result.Ok(SynchronousMessages)
        else -> Result.Err("'$str' is not a valid V4 interaction type")
      }
    }
  }
}
