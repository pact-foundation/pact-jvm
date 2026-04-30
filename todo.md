# TODO

## PR #1901 — Follow-up items

### 1. Fix `RandomAccessFile` leak if `channel.lock()` throws

In `core/model/src/main/kotlin/au/com/dius/pact/core/model/PactWriter.kt`:

```kotlin
val raf = RandomAccessFile(pactFile, "rw")
val lock = raf.channel.lock()   // if this throws, raf is never closed
try { ... } finally {
    lock.release()
    raf.close()
}
```

If `lock()` throws (e.g. `OverlappingFileLockException`), `raf` is leaked. Fix by wrapping `raf` in its own `use {}` block or moving the lock acquisition inside the `try`.

### 2. Guard against `pactFile.parentFile` being null

In the same file, `pactFile.parentFile.mkdirs()` is now called unconditionally. If `pactFile` has no parent (e.g. `File("pact.json")`), `parentFile` returns `null` and `mkdirs()` throws an NPE. Add a null guard:

```kotlin
pactFile.parentFile?.mkdirs()
```

### 3. Add a test for first-write in MERGE mode

No test covers the case where MERGE mode writes to a file that does not yet exist (or is empty). This is the exact scenario the PR fixes. A regression test should be added to `core/model` to verify that:
- Writing in MERGE mode when no pact file exists creates the file correctly
- A second write in MERGE mode merges into the existing file
- Concurrent writes in MERGE mode do not corrupt the file
