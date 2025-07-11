package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.support.Result
import java.util.Base64

//  /// If the result is an error result
//  pub fn is_err(&self) -> bool {
//    match self {
//      NodeResult::ERROR(_) => true,
//      _ => false
//    }
//  }
//
//  /// If the result is an ok result
//  pub fn is_ok(&self) -> bool {
//    match self {
//      NodeResult::OK => true,
//      _ => false
//    }
//  }
//}

/** Enum to store the result of executing a node */
sealed class NodeResult {
  /** Default value to make a node as successfully executed */
  data object OK: NodeResult() {
    override fun toString() = "OK"
  }

  /** Marks a node as successfully executed with a result */
  data class VALUE(val value: NodeValue): NodeResult() {
    override fun toString() = value.strForm()
  }

  /** Marks a node as unsuccessfully executed with an error */
  data class ERROR(val message: String): NodeResult() {
    override fun toString() = "ERROR($message)"

    override fun strForm() = "ERROR(${message.replace("(", "\\(")
      .replace(")", "\\)")})"
  }

  /** If this value represents a truthy value (not NULL, false ot empty) */
  fun isTruthy(): Boolean {
    return when (this) {
      is OK -> true
      is VALUE -> when (this.value) {
        is NodeValue.BARRAY -> this.value.bytes.isNotEmpty()
        is NodeValue.BOOL -> this.value.bool
        is NodeValue.LIST -> this.value.items.isNotEmpty()
        is NodeValue.MMAP -> this.value.entries.isNotEmpty()
        is NodeValue.SLIST -> this.value.items.isNotEmpty()
        is NodeValue.STRING -> this.value.string.isNotEmpty()
        is NodeValue.UINT -> this.value.uint.toInt() != 0
        else -> false
      }
      is ERROR -> false
    }
  }

  /** Converts this node result into a truthy value */
  fun truthy() = VALUE(NodeValue.BOOL(isTruthy()))

  /** Return the AND of this result with the given one */
  fun and(result: NodeResult?): NodeResult {
    return if (result != null) {
      when (this) {
        is ERROR -> this
        OK -> when (result) {
          is ERROR -> result
          OK -> OK
          is VALUE -> result
        }
        is VALUE -> when (result) {
          is ERROR -> result
          OK -> this
          is VALUE -> VALUE(this.value.and(result.value))
        }
      }
    } else {
      this
    }
  }

  /** Return the OR of this result with the given one */
  fun or(result: NodeResult?): NodeResult {
    return if (result != null) {
      when (this) {
        OK -> this
        is ERROR -> result
        is VALUE -> when (result) {
          is ERROR -> this
          OK -> this
          is VALUE -> VALUE(this.value.or(result.value))
        }
      }
    } else {
      this
    }
  }


  /** Unwraps the result into a value, or returns the error result as an error */
  fun valueOrError(): Result<NodeValue, String> {
    return when (this) {
      is ERROR -> Result.Err(message)
      OK -> Result.Ok(NodeValue.BOOL(true))
      is VALUE -> Result.Ok(value)
    }
  }

  /** Converts the result value to a string */
  @Suppress("CyclomaticComplexMethod")
  fun asString(): String? {
    return when (this) {
      OK -> null
      is VALUE -> {
        when (val v = value) {
          is NodeValue.BARRAY -> Base64.getEncoder().encodeToString(v.bytes)
          is NodeValue.BOOL -> v.bool.toString()
          is NodeValue.ENTRY -> "${v.key} -> ${v.value}"
          is NodeValue.JSON -> v.json.serialise()
          is NodeValue.LIST -> v.items.toString()
          is NodeValue.MMAP -> v.entries.toString()
          is NodeValue.NAMESPACED -> "${v.name}:${v.value}"
          NodeValue.NULL -> ""
          is NodeValue.SLIST -> v.items.toString()
          is NodeValue.STRING -> v.string
          is NodeValue.UINT -> v.uint.toString()
          is NodeValue.XML -> v.toString()
        }
      }
      is ERROR -> null
    }
  }

  /** Returns the associated value if there is one */
  fun asValue(): NodeValue? {
    return if (this is VALUE) {
      value
    } else {
      null
    }
  }

  /** If the result is a number, returns it */
  fun asNumber(): UInt? {
    return if (this is VALUE) {
      if (this.value is NodeValue.UINT) {
        this.value.uint
      } else {
        null
      }
    } else {
      null
    }
  }

  /** If the result is a list of Strings, returns it */
  fun asSList(): List<String>? {
    return if (this is VALUE) {
      if (this.value is NodeValue.SLIST) {
        this.value.items
      } else {
        null
      }
    } else {
      null
    }
  }

  /** Safe form to put in the plan output */
  open fun strForm(): String = this.toString()
}

public fun NodeResult?.or(result: NodeResult) = this?.or(result) ?: result
public fun NodeResult?.orDefault() = this ?: NodeResult.OK
