package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Any vertex that can exist in an abstract syntax tree.
 */
abstract class ASTVertex(val order: Int) : WithinMethod() {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.WITHIN_METHOD
        )
    }
}