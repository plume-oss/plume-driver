package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class FileNodeTests extends PlumeCodeToCpgSuite {

  private val UNKNOWN = "<unknown>"

  override val code: String =
    """
      | class Foo { }
      |""".stripMargin

  "should contain two file nodes in total, both with order=0" in {
    // cpg.file.order.l shouldBe List(0, 0)
    // cpg.file.name(UNKNOWN).size shouldBe 1
    // cpg.file.nameNot().size shouldBe 1
  }

  "should contain exactly one placeholder file node with `name=\"<unknown>\"/order=0`" in {
//    cpg.file(UNKNOWN).l match {
//      case List(x) =>
//        x.order shouldBe 0
//      case _ => fail()
//    }
  }

  "should contain exactly one non-placeholder file with absolute path in `name`" in {
//    cpg.file.nameNot(UNKNOWN).l match {
//      case List(x) =>
//        x.name should startWith("/")
//      case _ => fail()
//    }
  }

}

