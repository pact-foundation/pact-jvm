package au.com.dius.pact.consumer

object PrettyPrinter {
  def print(session: PactSessionResults): String = session.toString

}
