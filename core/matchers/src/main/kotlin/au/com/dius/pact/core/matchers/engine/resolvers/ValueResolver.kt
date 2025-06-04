package au.com.dius.pact.core.matchers.engine.resolvers

import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.support.Result

interface ValueResolver {
  /** Resolve the path expression against the test context */
  fun resolve(path: DocPath, context: PlanMatchingContext): Result<NodeValue, String>
}
