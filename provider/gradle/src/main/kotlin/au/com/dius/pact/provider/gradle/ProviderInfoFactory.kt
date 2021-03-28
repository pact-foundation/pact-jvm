package au.com.dius.pact.provider.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

class ProviderInfoFactory(val project: Project) : NamedDomainObjectFactory<GradleProviderInfo> {
  override fun create(name: String): GradleProviderInfo {
    return GradleProviderInfo(name, project)
  }
}
