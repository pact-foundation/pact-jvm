package au.com.dius.pact.provider;

object ServiceInvokeRequest {
  def apply(url: String, request: Request):Request = {
    val r = request.copy
    r.setPath(s"$url${request.getPath}")
    r
  }
}
