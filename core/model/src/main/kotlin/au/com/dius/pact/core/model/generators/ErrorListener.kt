package au.com.dius.pact.core.model.generators

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class ErrorListener(val errors: MutableList<String> = mutableListOf()) : BaseErrorListener() {
  override fun syntaxError(
    recognizer: Recognizer<*, *>,
    offendingSymbol: Any,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    errors.add("line $line:$charPositionInLine $msg")
  }
}
