package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class TypeInterproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "interprocedural/type"

        init {
            val testFileUrl = TypeInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Type$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f).project()
        g = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g.close()
    }

    @Test
    fun type1Test() {
        val ns = g.nodes().asSequence().toList()
        ns.filterIsInstance<Local>().let { localList ->
            assertNotNull(localList.firstOrNull { it.name() == "intList" && it.typeFullName() == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name() == "stringList" && it.typeFullName() == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name() == "\$stack3" && it.typeFullName() == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name() == "\$stack4" && it.typeFullName() == "java.util.LinkedList" })
        }
        ns.filterIsInstance<Call>().filter { it.name() == "<init>" }.let { callList ->
            assertNotNull(callList.firstOrNull { it.methodFullName() == "java.util.LinkedList.<init>:void()" })
            assertNotNull(callList.firstOrNull { it.methodFullName() == "java.lang.Object.<init>:void()" })
        }
        ns.filterIsInstance<TypeDecl>().filter { it.fullName() == "java.util.LinkedList" }
            .let { typeRefs -> assertEquals(1, typeRefs.toList().size) }
    }

    @Test
    fun type2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(
            ns.filterIsInstance<Local>().firstOrNull { it.name() == "intArray" && it.typeFullName() == "int[]" })
        val indexAccesses = ns.filterIsInstance<Call>().filter { it.name() == Operators.indexAccess }
        assertEquals(2, indexAccesses.size)
        indexAccesses.forEach { acc ->
            val inCFG = acc.`in`(CFG).asSequence().toList()
            assertTrue(inCFG.filterIsInstance<Literal>().isNotEmpty())
        }
        assertEquals(3, ns.filterIsInstance<Identifier>().filter { it.name().contains("intArray") }.size)
    }

    @Test
    fun type3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(
            ns.filterIsInstance<Local>()
                .firstOrNull { it.name() == "cls" && it.typeFullName() == "java.lang.Class" })
        assertNotNull(
            ns.filterIsInstance<Literal>()
                .firstOrNull {
                    it.code() == "class \"Lintraprocedural/type/Type3;\"" && it.typeFullName() == "java.lang.Class"
                })
    }

    @Test
    fun type4Test() {
        val ns = g.nodes().asSequence().toList()
        val io = ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.instanceOf }
        assertNotNull(io); io!!
        val x = io.`in`().asSequence().toList().filterIsInstance<Identifier>().firstOrNull { it.code() == "x" }
        assertNotNull(x); x!!
        val type = x._evalTypeOut().asSequence().firstOrNull()
        assertNotNull(type); type!!
        assertEquals("java.util.LinkedList", (type as Type).fullName())
    }
}