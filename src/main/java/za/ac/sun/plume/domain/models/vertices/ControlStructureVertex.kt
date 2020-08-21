package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A control structure such as if, while, or for
 */
class ControlStructureVertex(
        val name: String,
        code: String,
        lineNumber: Int,
        order: Int,
        argumentIndex: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.CONTROL_STRUCTURE
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ControlStructureVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "ControlStructureVertex(name='$name', order='$order')"
    }

}