package au.com.dius.pact.model

import au.com.dius.pact.model.matchingrules.MatchingRules

/**
 * Base trait for an object that represents part of an http message
 */
trait HttpPart {

  private static final String CONTENT_TYPE = 'Content-Type'

  abstract OptionalBody getBody()
  abstract Map<String, String> getHeaders()
  abstract void setHeaders(Map<String, String> headers)
  abstract MatchingRules getMatchingRules()

  String mimeType() {
    def contentTypeKey = headers?.keySet()?.find { CONTENT_TYPE.equalsIgnoreCase(it) }
    if (contentTypeKey) {
      headers[contentTypeKey].split('\\s*;\\s*').first()
    } else {
      detectContentType()
    }
  }

  boolean jsonBody() {
    mimeType().matches('application\\/.*json')
  }

  boolean xmlBody() {
    mimeType().matches('application\\/.*xml')
  }

  static final XMLREGEXP = /^\s*<\?xml\s*version.*/
  static final HTMLREGEXP = /^\s*(<!DOCTYPE)|(<HTML>).*/
  static final JSONREGEXP = /^\s*(true|false|null|[0-9]+|"\w*|\{\s*(}|"\w+)|\[\s*).*/
  static final XMLREGEXP2 = /^\s*<\w+\s*(:\w+=[\"”][^\"”]+[\"”])?.*/

  String detectContentType() {
    if (body.present) {
      def s = body.value[0..Math.min(body.value.size(), 32) - 1].replaceAll('\n', '')
      if (s ==~ XMLREGEXP) {
        'application/xml'
      } else if (s.toUpperCase() ==~ HTMLREGEXP) {
        'text/html'
      } else if (s ==~ JSONREGEXP) {
        'application/json'
      } else if (s ==~ XMLREGEXP2) {
        'application/xml'
      } else {
        'text/plain'
      }
    } else {
      'text/plain'
    }
  }

  void setDefaultMimeType(String mimetype) {
    if (headers == null) {
      headers = [:]
    }
    if (!headers.containsKey(CONTENT_TYPE)) {
      headers[CONTENT_TYPE] = mimetype
    }
  }
}
