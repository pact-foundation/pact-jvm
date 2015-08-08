/*
 * Copyright 2012 Copyright 2012 Typesafe Inc. <http://www.typesafe.com>
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

import org.slf4j.LoggerFactory

/**
 * Requires the member `logger` of type [[$Logger]] to be defined in the class into which this trait is mixed.
 *
 * @define Logger au.com.dius.pact.com.typesafe.scalalogging.Logger
 */
trait AbstractLogging {

  protected def logger: Logger
}

/**
 * Adds the lazy val `logger` of type [[$Logger]] to the class into which this trait is mixed.
 *
 * If you need a non-lazy [[$Logger]], which would probably be a special case,
 * use [[au.com.dius.pact.com.typesafe.scalalogging.StrictLogging]].
 *
 * @define Logger au.com.dius.pact.com.typesafe.scalalogging.Logger
 */
trait Logging extends AbstractLogging {

  protected lazy val logger: Logger =
    Logger(LoggerFactory getLogger getClass.getName)
}

/**
 * Adds the non-lazy val `logger` of type [[$Logger]] to the class into which this trait is mixed.
 *
 * If you need a lazy [[$Logger]], which would probably be preferrable,
 * use [[au.com.dius.pact.com.typesafe.scalalogging.Logging]].
 *
 * @define Logger au.com.dius.pact.com.typesafe.scalalogging.Logger
 */
trait StrictLogging extends AbstractLogging {

  protected val logger: Logger =
    Logger(LoggerFactory getLogger getClass.getName)
}
