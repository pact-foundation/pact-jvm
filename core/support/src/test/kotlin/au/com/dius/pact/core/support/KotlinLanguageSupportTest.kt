package au.com.dius.pact.core.support

import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec

class KotlinLanguageSupportTest : DescribeSpec({
  describe("Zip All") {
    it("returns an empty list when both lists are empty") {
      emptyList<Int>().zipAll(emptyList()).shouldBeEmpty()
    }

    it("returns a correctly sized list when the other list is empty") {
      val list = listOf(1, 2, 3).zipAll(emptyList())
      list.shouldHaveSize(3)
      list.forEachIndexed { index, pair ->
        pair.first.shouldBe(index + 1)
        pair.second.shouldBeNull()
      }
    }

    it("returns a correctly sized list when the list is empty") {
      val list = emptyList<Int>().zipAll(listOf(1, 2, 3))
      list.shouldHaveSize(3)
      list.forEachIndexed { index, pair ->
        pair.first.shouldBeNull()
        pair.second.shouldBe(index + 1)
      }
    }

    it("returns a correctly sized list when the lists have the same size") {
      val list = listOf(1, 2, 3).zipAll(listOf(2, 4, 6))
      list.shouldHaveSize(3)
      list.forEachIndexed { index, pair ->
        pair.first.shouldBe(index + 1)
        pair.second.shouldBe(2 * (index + 1))
      }
    }

    it("returns a correctly sized list when the other list is smaller") {
      val list = listOf(1, 2, 3).zipAll(listOf(2, 4))
      list.shouldHaveSize(3)
      list.forEachIndexed { index, pair ->
        pair.first.shouldBe(index + 1)
        if (index >= 2) {
          pair.second.shouldBeNull()
        } else {
          pair.second.shouldBe(2 * (index + 1))
        }
      }
    }

    it("returns a correctly sized list when the other list is bigger") {
      val list = listOf(1, 2, 3).zipAll(listOf(1, 2, 3, 4))
      list.shouldHaveSize(4)
      list.forEachIndexed { index, pair ->
        if (index >= 3) {
          pair.first.shouldBeNull()
        } else {
          pair.first.shouldBe(index + 1)
        }
        pair.second.shouldBe(index + 1)
      }
    }
  }
})
