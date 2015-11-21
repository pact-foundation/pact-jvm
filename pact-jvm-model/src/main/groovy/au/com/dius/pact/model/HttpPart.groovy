package au.com.dius.pact.model

/**
 * Base trait for an object that represents part of an http message
 */
trait HttpPart {

  private static String CONTENT_TYPE = 'Content-Type'

  abstract String getBody()
  abstract Map<String, String> getHeaders()
  abstract void setHeaders(Map<String, String> headers)
  abstract Map<String, Map<String, Object>> getMatchingRules()

  String mimeType() {
    if (headers?.containsKey(CONTENT_TYPE)) {
      headers[CONTENT_TYPE].split('\\s*;\\s*').first()
    } else {
      detectContentType()
    }
  }

  boolean jsonBody() {
    mimeType().matches('application\\/.*json')
  }

  static XMLREGEXP = /^\s*<\?xml\s*version.*/
  static HTMLREGEXP = /^\s*(<!DOCTYPE)|(<HTML>).*/
  static JSONREGEXP = /^\s*("(\.|[^"\\u000A\u000D])*?"|[,:{}\[\]0-9.\-+Eaeflnr-u \u000A\u000D\u0009])+/
  static XMLREGEXP2 = /^\s*<\w+\s*(:\w+=[\"”][^\"”]+[\"”])?.*/

  String detectContentType() {
    if (body) {
      def s = body.substring(0, Math.min(body.size(), 32)).replaceAll('\n', '')
      if (s ==~ XMLREGEXP) {
        "application/xml"
      } else if (s.toUpperCase() ==~ HTMLREGEXP) {
        "text/html"
      } else if (s ==~ JSONREGEXP) {
        "application/json"
      } else if (s ==~ XMLREGEXP2) {
        "application/xml"
      } else {
        "text/plain"
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
