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

  describe("padTo") {
    it("returns an empty list when if the array is empty") {
      emptyArray<Int>().padTo(100).shouldBeEmpty()
    }

    it("returns the list if the array is bigger than the pad") {
      arrayOf(1, 2, 3, 4).padTo(2).shouldHaveSize(4)
    }

    it("returns the list if the array is has the same size as the pad") {
      arrayOf(1, 2, 3, 4).padTo(4).shouldHaveSize(4)
    }

    it("pads the array by cycling the elements") {
      arrayOf(1, 2, 3, 4).padTo(8).shouldBe(listOf(1, 2, 3, 4, 1, 2, 3, 4))
      arrayOf(1, 2, 3, 4).padTo(5).shouldBe(listOf(1, 2, 3, 4, 1))
    }
  }
})
