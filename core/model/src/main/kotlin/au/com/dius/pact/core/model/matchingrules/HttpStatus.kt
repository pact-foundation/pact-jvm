package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.support.json.JsonValue

/** Class of HTTP statuses */
enum class HttpStatus {
  /** Informational responses (100–199) */
  Information,
  /** Successful responses (200–299) */
  Success,
  /** Redirects (300–399) */
  Redirect,
  /** Client errors (400–499) */
  ClientError,
  /** Server errors (500–599) */
  ServerError,
  /** Explicit list of status codes */
  StatusCodes,
  /** Non-error response(< 400) */
  NonError,
  /** Any error response (>= 400) */
  Error;

  fun toJson(values: List<Int>): Any {
    return when (this) {
      Information -> "info"
      Success -> "success"
      Redirect -> "redirect"
      ClientError -> "clientError"
      ServerError -> "serverError"
      StatusCodes -> values
      NonError -> "nonError"
      Error -> "error"
    }
  }

  override fun toString(): String {
    return when (this) {
      Information -> "Informational response (100–199)"
      Success -> "Successful response (200–299)"
      Redirect -> "Redirect (300–399)"
      ClientError -> "Client error (400–499)"
      ServerError -> "Server error (500–599)"
      NonError -> "Non-error response (< 400)"
      Error -> "Error response (>= 400)"
      else -> super.toString()
    }
  }

  companion object {
    fun fromJson(value: JsonValue): HttpStatus {
      return if (value.isString) {
        when (value.asString()!!) {
          "info" -> Information
          "success" -> Success
          "redirect" -> Redirect
          "clientError" -> ClientError
          "serverError" -> ServerError
          "nonError" -> NonError
          "error" -> Error
          else -> throw InvalidMatcherJsonException("Invalid Status code matcher JSON: $value")
        }
      } else {
        throw InvalidMatcherJsonException("Invalid Status code matcher JSON: $value")
      }
    }
  }
}
