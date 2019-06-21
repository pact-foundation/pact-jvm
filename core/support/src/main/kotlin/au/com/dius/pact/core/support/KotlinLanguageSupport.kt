package au.com.dius.pact.core.support

public fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

public fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false
