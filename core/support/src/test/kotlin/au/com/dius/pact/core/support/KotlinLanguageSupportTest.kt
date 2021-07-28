package au.com.dius.pact.core.support

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
}
