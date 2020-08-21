package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A return instruction
 */
class ReturnVertex(
        lineNumber: Int,
        order: Int,
        argumentIndex: Int,
        code: String
) : ExpressionVertex(code, argumentIndex, lineNumber, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.RETURN
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReturnVertex) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun toString(): String {
        return "ReturnVertex()"
    }
}