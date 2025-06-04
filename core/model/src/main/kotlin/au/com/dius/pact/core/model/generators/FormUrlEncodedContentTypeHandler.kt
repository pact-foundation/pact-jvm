package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.parsePath
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.WWWFormCodec
import java.nio.charset.Charset

object FormUrlEncodedContentTypeHandler : ContentTypeHandler {
  override fun processBody(value: OptionalBody, fn: (QueryResult) -> Unit): OptionalBody {
    val charset = value.contentType.asCharset()
    val body = FormQueryResult(WWWFormCodec.parse(value.valueAsString(), charset))
    fn.invoke(body)
    return OptionalBody.body(WWWFormCodec.format(body.body, charset).toByteArray(charset), value.contentType)
  }

  override fun applyKey(body: QueryResult, key: String, generator: Generator, context: MutableMap<String, Any>) {
    val values = (body as FormQueryResult).body
    val pathExp = parsePath(key)
    values.forEachIndexed { index, entry ->
      if (pathMatches(pathExp, entry.name.orEmpty())) {
        values[index] = BasicNameValuePair(entry.name, generator.generate(context, entry.value)?.toString())
      }
    }
  }

  private fun pathMatches(pathExp: List<PathToken>, name: String): Boolean {
    val root = pathExp[0]
    val levelOne = pathExp[1]
    return pathExp.size == 2 && root is PathToken.Root &&
      (levelOne is PathToken.Star || (levelOne is PathToken.Field && levelOne.name == name))
  }
}

class FormQueryResult(
  var body: MutableList<NameValuePair>,
  override val key: Any? = null,
  override val path: List<String> = emptyList()
) : QueryResult {
  override var value: Any?
    get() = body
    set(value) {
      body = if (value is List<*>) {
        (value as List<NameValuePair>).toMutableList()
      } else {
        WWWFormCodec.parse(value.toString(), Charset.defaultCharset())
      }
    }
}
