package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Any vertex that can exist in a method.
 */
abstract class WithinMethod : PlumeVertex {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.WITHIN_METHOD
        )
    }
}