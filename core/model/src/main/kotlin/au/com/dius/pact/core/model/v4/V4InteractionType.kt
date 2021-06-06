package au.com.dius.pact.core.model.v4

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

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
        "Synchronous/HTTP" -> Ok(SynchronousHTTP)
        "Asynchronous/Messages" -> Ok(AsynchronousMessages)
        "Synchronous/Messages" -> Ok(SynchronousMessages)
        else -> Err("'$str' is not a valid V4 interaction type")
      }
    }
  }
}
