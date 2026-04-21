package au.com.dius.pact.core.matchers.engine.resolvers

import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Result

class MessageValueResolver(
  val contents: MessageContents
) : ValueResolver {
  @Suppress("CyclomaticComplexity")
  override fun resolve(path: DocPath, context: PlanMatchingContext): Result<NodeValue, String> {
    val field = path.firstField()
    return if (field != null) {
      when (field) {
        "body" -> if (path.len() == 2 && contents.contents.isPresent()) {
          Result.Ok(NodeValue.BARRAY(contents.contents.unwrap()))
        } else {
          Result.Ok(NodeValue.NULL)
        }
        "content-type" -> Result.Ok(NodeValue.STRING(contents.getContentType().toString()))
        "metadata" -> {
          if (path.len() == 2 || (path.len() == 3 && path.isWildcard())) {
            Result.Ok(NodeValue.MMAP(contents.metadata.mapValues { listOf(it.value?.toString() ?: "") }))
          } else if (path.len() == 3) {
            val key = path.lastField()
            if (key != null) {
              val value = contents.metadata[key]
              if (value != null) {
                Result.Ok(NodeValue.STRING(value.toString()))
              } else {
                Result.Ok(NodeValue.NULL)
              }
            } else {
              Result.Ok(NodeValue.NULL)
            }
          } else {
            Result.Err("$path is not valid for message metadata")
          }
        }
        else -> Result.Err("$path is not valid for a message")
      }
    } else {
      Result.Err("$path is not valid for a message")
    }
  }
}
