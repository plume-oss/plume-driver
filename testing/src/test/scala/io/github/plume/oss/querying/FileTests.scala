package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._
import io.shiftleft.semanticcpg.language.types.structure.File
import java.io.{File => JFile}

class FileTests extends PlumeCodeToCpgSuite {

  override val code: String =
    """
      | package a.b;
      | class Foo { int bar() { return 1; } }
      |""".stripMargin

  "should contain four file nodes in total with order=1 (java.lang.Object|Class|String and a.b.Foo)" in {
    cpg.file.order.l shouldBe List(0, 1, 1, 1, 1)
    cpg.file.name(File.UNKNOWN).size shouldBe 1
    cpg.file.nameNot(File.UNKNOWN).size shouldBe 4
  }

  "should contain exactly four non-placeholder file with absolute path in `name`" in {
    val List(u, v, _, _) = cpg.file.nameNot(File.UNKNOWN).l
    u.name should startWith(JFile.separator)
    u.hash.isDefined shouldBe true
    v.name should startWith(JFile.separator)
    v.hash.isDefined shouldBe false
  }

  "should allow traversing from file to its namespace blocks" in {
    cpg.file.nameNot(File.UNKNOWN).namespaceBlock.name.toSet shouldBe Set("a.b", "java.lang")
  }

  "should allow traversing from file to its methods via namespace block" in {
    cpg.file.name("/a/b/Foo.class".replace("/", s"\\${JFile.separator}")).method.name.toSet shouldBe Set("<init>", "bar")
  }

  "should allow traversing from file to its type declarations via namespace block" in {
    cpg.file.nameNot(File.UNKNOWN).typeDecl.name.toSet shouldBe Set("Foo", "Object", "Class", "String")
  }

}

