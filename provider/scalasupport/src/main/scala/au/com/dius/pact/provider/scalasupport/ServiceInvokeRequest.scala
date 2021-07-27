package au.com.dius.pact.provider.scalasupport

import au.com.dius.pact.core.model.IRequest

object ServiceInvokeRequest {
  def apply(url: String, request: IRequest): IRequest = {
    val r = request.copy
    r.setPath(s"$url${request.getPath}")
    r
  }
}
