package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.json.JsonValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test

class KotlinLanguageSupportTest {
  @Test
  fun zipAllReturnsAnEmptyListWhenBothListsAreEmpty() {
    assertThat(emptyList<Int>().zipAll(emptyList()), `is`(empty()))
  }

  @Test
  fun zipAllReturnsACorrectlySizedListWhenTheOtherListIsEmpty() {
    val list = listOf(1, 2, 3).zipAll(emptyList())
    assertThat(list, hasSize(3))
    list.forEachIndexed { index, pair ->
      assertThat(pair.first, `is`(index + 1))
      assertThat(pair.second, `is`(nullValue()))
    }
  }

  @Test
  fun zipAllReturnsACorrectlySizedListWhenTheListIsEmpty() {
    val list = emptyList<Int>().zipAll(listOf(1, 2, 3))
    assertThat(list, hasSize(3))
    list.forEachIndexed { index, pair ->
      assertThat(pair.first, `is`(nullValue()))
      assertThat(pair.second, `is`(index + 1))
    }
  }

  @Test
  fun zipAllReturnsACorrectlySizedListWhenTheListsHaveTheSameSize() {
    val list = listOf(1, 2, 3).zipAll(listOf(2, 4, 6))
    assertThat(list, hasSize(3))
    list.forEachIndexed { index, pair ->
      assertThat(pair.first, `is`(index + 1))
      assertThat(pair.second, `is`(2 * index + 2))
    }
  }

  @Test
  fun zipAllReturnsACorrectlySizedListWhenTheOtherListIsSmaller() {
    val list = listOf(1, 2, 3).zipAll(listOf(2, 4))
    assertThat(list, hasSize(3))
    list.forEachIndexed { index, pair ->
      assertThat(pair.first, `is`(index + 1))
      if (index >= 2) {
        assertThat(pair.second, `is`(nullValue()))
      } else {
        assertThat(pair.second, `is`(2 * index + 2))
      }
    }
  }

  @Test
  fun zipAllReturnsACorrectlySizedListWhenTheOtherListIsBigger() {
    val list = listOf(1, 2, 3).zipAll(listOf(1, 2, 3, 4))
    assertThat(list, hasSize(4))
    list.forEachIndexed { index, pair ->
      if (index >= 3) {
        assertThat(pair.first, `is`(nullValue()))
      } else {
        assertThat(pair.first, `is`(index + 1))
      }
      assertThat(pair.second, `is`(index + 1))
    }
  }

  @Test
  fun padToReturnsAnEmptyListWhenIfTheArrayIsEmpty() {
    assertThat(emptyArray<Int>().padTo(100), `is`(empty()))
  }

  @Test
  fun padToReturnsTheListIfTheArrayIsBiggerThanThePad() {
    assertThat(arrayOf(1, 2, 3, 4).padTo(2), hasSize(4))
  }

  @Test
  fun padToReturnsTheListIfTheArrayIsHasTheSameSizeAsThePad() {
    assertThat(arrayOf(1, 2, 3, 4).padTo(4), hasSize(4))
  }

  @Test
  fun padToPadsTheArrayByCyclingTheElements() {
    assertThat(arrayOf(1, 2, 3, 4).padTo(8), `is`(listOf(1, 2, 3, 4, 1, 2, 3, 4)))
    assertThat(arrayOf(1, 2, 3, 4).padTo(5), `is`(listOf(1, 2, 3, 4, 1)))
  }

  @Test
  fun deepMergeHandlesNull() {
    val map: MutableMap<String, JsonValue>? = null
    assertThat(map.deepMerge(mapOf()), `is`(emptyMap()))
  }

  @Test
  fun deepMergeWithEmptyMaps() {
    val map: MutableMap<String, JsonValue> = mutableMapOf()
    assertThat(map.deepMerge(mapOf()), `is`(emptyMap()))
    assertThat(map.deepMerge(mapOf("a" to JsonValue.Null)), `is`(mapOf("a" to JsonValue.Null)))
    val map1: MutableMap<String, JsonValue> = mutableMapOf("a" to JsonValue.Null)
    assertThat(map1.deepMerge(mapOf()), `is`(mapOf("a" to JsonValue.Null)))
  }

  @Test
  fun deepMergeWithSimpleMaps() {
    val map: MutableMap<String, JsonValue> = mutableMapOf("a" to JsonValue.Null)
    assertThat(map.deepMerge(mapOf("b" to JsonValue.True)), `is`(mapOf("a" to JsonValue.Null, "b" to JsonValue.True)))
    assertThat(map.deepMerge(mapOf("b" to JsonValue.True, "a" to JsonValue.False)),
      `is`(mapOf("a" to JsonValue.False, "b" to JsonValue.True)))
  }

  @Test
  fun deepMergeWithCollectionsWithDifferentTypes() {
    val map: MutableMap<String, JsonValue> = mutableMapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.True)),
      "b" to JsonValue.Array(mutableListOf(JsonValue.True))
    )
    assertThat(map.deepMerge(mapOf("a" to JsonValue.True)), `is`(mapOf(
      "a" to JsonValue.True, "b" to JsonValue.Array(mutableListOf(JsonValue.True)))))
    assertThat(map.deepMerge(mapOf("b" to JsonValue.True)), `is`(mapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.True)), "b" to JsonValue.True)))
  }

  @Test
  fun deepMergeWithCollectionsRecursivelyMergesTheCollections() {
    val map: MutableMap<String, JsonValue> = mutableMapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.True)),
      "b" to JsonValue.Array(mutableListOf(JsonValue.True))
    )
    val map2: MutableMap<String, JsonValue> = mutableMapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.False)),
      "b" to JsonValue.Array(mutableListOf(JsonValue.False))
    )
    assertThat(map.deepMerge(map2), `is`(mapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.False)),
      "b" to JsonValue.Array(mutableListOf(JsonValue.True, JsonValue.False)))))
    assertThat(map.deepMerge(map), `is`(mapOf(
      "a" to JsonValue.Object(mutableMapOf("b" to JsonValue.True)),
      "b" to JsonValue.Array(mutableListOf(JsonValue.True, JsonValue.True)))))
  }
}
