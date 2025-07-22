package au.com.dius.pact.core.matchers.engine.resolvers

import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.support.Result

class HttpRequestValueResolver(
  /** Request to resolve values against */
  val request: IRequest
): ValueResolver {
  @Suppress("LongMethod", "CyclomaticComplexMethod")
  override fun resolve(path: DocPath, context: PlanMatchingContext): Result<NodeValue, String> {
    val field = path.firstField()
    return if (field != null) {
      when (field) {
        "method" -> Result.Ok(NodeValue.STRING(request.method))
        "path" -> {
          val match = URL_RE.find(request.path)
          if (match != null) {
            Result.Ok(NodeValue.STRING(request.path.replaceFirst(match.value, "")))
          } else {
            Result.Ok(NodeValue.STRING(request.path))
          }
        }
        "query" -> {
          if (path.len() == 2 || (path.len() == 3 && path.isWildcard())) {
            Result.Ok(NodeValue.MMAP(request.query.mapValues { entry -> entry.value.map { it ?: "" } }))
          } else if (path.len() == 3) {
            val paramName = path.lastField()
            if (paramName != null) {
              val values = request.query[paramName]
              if (values != null && values.size == 1) {
                Result.Ok(NodeValue.STRING(values[0] ?: ""))
              } else if (values != null) {
                Result.Ok(NodeValue.SLIST(values.map { it ?: "" }))
              } else {
                Result.Ok(NodeValue.NULL)
              }
            } else {
              Result.Ok(NodeValue.NULL)
            }
          } else {
            Result.Err("$path is not valid for a HTTP request query parameters")
          }
        }
        "headers" -> {
          val headers = request.headers.mapKeys { it.key.lowercase() }
          if (path.len() == 2 || (path.len() == 3 && path.isWildcard())) {
            Result.Ok(NodeValue.MMAP(headers))
          } else if (path.len() == 3) {
            val paramName = path.lastField()?.lowercase()
            if (paramName != null) {
              val values = headers[paramName]
              if (values != null && values.size == 1) {
                Result.Ok(NodeValue.STRING(values[0]))
              } else if (values != null) {
                Result.Ok(NodeValue.SLIST(values))
              } else {
                Result.Ok(NodeValue.NULL)
              }
            } else {
              Result.Ok(NodeValue.NULL)
            }
          } else if (path.len() == 4 && path.last()!!.isIndex()) {
            val paramName = path.lastField()?.lowercase()
            if (paramName != null && headers.containsKey(paramName)) {
              val last = path.last()
              if (last is PathToken.Index) {
                Result.Ok(NodeValue.STRING(headers[paramName]?.get(last.index) ?: ""))
              } else {
                Result.Ok(NodeValue.NULL)
              }
            } else {
              Result.Ok(NodeValue.NULL)
            }
          } else {
            Result.Err("$path is not valid for a HTTP request headers")
          }
        }
        "content-type" -> Result.Ok(NodeValue.STRING(request.determineContentType().toString()))
        "body" -> if (path.len() == 2 && request.body.isPresent()) {
          Result.Ok(NodeValue.BARRAY(request.body.unwrap()))
        } else {
          Result.Ok(NodeValue.NULL)
        }
        else -> Result.Err("$path is not valid for a HTTP request")
      }
    } else {
      Result.Err("$path is not valid for a HTTP request")
    }
  }

  companion object {
    val URL_RE = Regex("^https?://([^/]*)")
  }
}
