package com.dius.pact.author

import play.api.libs.json.{Json, DefaultWrites}

class JSONThingy extends DefaultWrites {
  def convert(obj: Map[String,List[String]]): String = Json.toJson(obj).toString()
}
