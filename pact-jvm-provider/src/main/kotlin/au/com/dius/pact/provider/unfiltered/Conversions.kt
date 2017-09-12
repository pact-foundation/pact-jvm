package au.com.dius.pact.provider.unfiltered

import com.ning.http.client.FluentCaseInsensitiveStringsMap

fun toMap(headers: FluentCaseInsensitiveStringsMap) : Map<String, String> =
  headers.mapValues { it.value.joinToString(separator = ", ") }
