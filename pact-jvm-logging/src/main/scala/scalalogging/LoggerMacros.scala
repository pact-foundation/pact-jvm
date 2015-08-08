/*
 * Copyright 2012 Typesafe Inc. <http://www.typesafe.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.dius.pact.com.typesafe.scalalogging

import org.slf4j.Marker
import scala.reflect.macros.Context

private object LoggerMacros {

  type LoggerContext = Context { type PrefixType = Logger }

  // Error

  def errorMessage(c: LoggerContext)(message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isErrorEnabled)
        c.prefix.splice.underlying.error(message.splice)
    )

  def errorMessageParams(c: LoggerContext)(message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, None)("error")

  def errorMessageThrowable(c: LoggerContext)(message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isErrorEnabled)
        c.prefix.splice.underlying.error(message.splice, t.splice)
    )

  def errorMarkerMessage(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isErrorEnabled)
        c.prefix.splice.underlying.error(marker.splice, message.splice)
    )

  def errorMarkerMessageParams(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, Some(marker))("error")

  def errorMarkerMessageThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isErrorEnabled)
        c.prefix.splice.underlying.error(marker.splice, message.splice, t.splice)
    )

  // Warn

  def warnMessage(c: LoggerContext)(message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isWarnEnabled)
        c.prefix.splice.underlying.warn(message.splice)
    )

  def warnMessageParams(c: LoggerContext)(message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, None)("warn")

  def warnMessageThrowable(c: LoggerContext)(message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isWarnEnabled)
        c.prefix.splice.underlying.warn(message.splice, t.splice)
    )

  def warnMarkerMessage(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isWarnEnabled)
        c.prefix.splice.underlying.warn(marker.splice, message.splice)
    )

  def warnMarkerMessageParams(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, Some(marker))("warn")

  def warnMarkerMessageThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isWarnEnabled)
        c.prefix.splice.underlying.warn(marker.splice, message.splice, t.splice)
    )

  // Info

  def infoMessage(c: LoggerContext)(message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isInfoEnabled)
        c.prefix.splice.underlying.info(message.splice)
    )

  def infoMessageParams(c: LoggerContext)(message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, None)("info")

  def infoMessageThrowable(c: LoggerContext)(message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isInfoEnabled)
        c.prefix.splice.underlying.info(message.splice, t.splice)
    )

  def infoMarkerMessage(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isInfoEnabled)
        c.prefix.splice.underlying.info(marker.splice, message.splice)
    )

  def infoMarkerMessageParams(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, Some(marker))("info")

  def infoMarkerMessageThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isInfoEnabled)
        c.prefix.splice.underlying.info(marker.splice, message.splice, t.splice)
    )

  // Debug

  def debugMessage(c: LoggerContext)(message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isDebugEnabled)
        c.prefix.splice.underlying.debug(message.splice)
    )

  def debugMessageParams(c: LoggerContext)(message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, None)("debug")

  def debugMessageThrowable(c: LoggerContext)(message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isDebugEnabled)
        c.prefix.splice.underlying.debug(message.splice, t.splice)
    )

  def debugMarkerMessage(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isDebugEnabled)
        c.prefix.splice.underlying.debug(marker.splice, message.splice)
    )

  def debugMarkerMessageParams(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, Some(marker))("debug")

  def debugMarkerMessageThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isDebugEnabled)
        c.prefix.splice.underlying.debug(marker.splice, message.splice, t.splice)
    )

  // Trace

  def traceMessage(c: LoggerContext)(message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isTraceEnabled)
        c.prefix.splice.underlying.trace(message.splice)
    )

  def traceMessageParams(c: LoggerContext)(message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, None)("trace")

  def traceMessageThrowable(c: LoggerContext)(message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isTraceEnabled)
        c.prefix.splice.underlying.trace(message.splice, t.splice)
    )

  def traceMarkerMessage(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isTraceEnabled)
        c.prefix.splice.underlying.trace(marker.splice, message.splice)
    )

  def traceMarkerMessageParams(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], params: c.Expr[AnyRef]*) =
    logParams(c)(message, params, Some(marker))("trace")

  def traceMarkerMessageThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], t: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.underlying.isTraceEnabled)
        c.prefix.splice.underlying.trace(marker.splice, message.splice, t.splice)
    )

  // Common

  private def logParams(
                         c: LoggerContext)(
                         message: c.Expr[String],
                         params: Seq[c.Expr[AnyRef]],
                         marker: Option[c.Expr[Marker]])(
                         level: String) = {
    import c.universe._
    val isEnabled = Select(
      Select(c.prefix.tree, newTermName("underlying")),
      newTermName(s"is${level.head.toUpper +: level.tail}Enabled")
    )
    val paramsWildcard = Typed(
      Apply(
        Ident(newTermName("List")),
        (params map (_.tree)).toList
      ),
      Ident(tpnme.WILDCARD_STAR)
    )
    val log = Apply(
      Select(Select(c.prefix.tree, newTermName("underlying")), newTermName(level)),
      marker.foldRight(message.tree +: List(paramsWildcard))(_.tree +: _)
    )
    c.Expr(If(isEnabled, log, Literal(Constant(()))))
  }
}
