package au.com.dius.pact.core.support

fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false
