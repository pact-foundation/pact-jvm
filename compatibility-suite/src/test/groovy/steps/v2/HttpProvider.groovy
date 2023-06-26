package steps.v2

import steps.shared.CompatibilitySuiteWorld

@SuppressWarnings('ThrowRuntimeException')
class HttpProvider {
  CompatibilitySuiteWorld world

  HttpProvider(CompatibilitySuiteWorld world) {
    this.world = world
  }
}
