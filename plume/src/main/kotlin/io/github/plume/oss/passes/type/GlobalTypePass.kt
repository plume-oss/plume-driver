/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.passes.type

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.ITypePass
import io.github.plume.oss.passes.structure.TypePass
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.RefType
import soot.Type

/**
 * Builds all types which will be considered as global types e.g. int, array types e.g. java.lang.String[].
 */
class GlobalTypePass(private val driver: IDriver) : ITypePass {

    private val logger: Logger = LogManager.getLogger(TypePass::javaClass)
    private val cache = DriverCache(driver)

    /**
     * Creates a global TYPE_DECL and connects it to the global namespace block. i.e
     *
     *     NAMESPACE_BLOCK(<global>) -(AST)-> TYPE_DECL
     *     FILE(<unknown>) -(CONTAINS)-> TYPE_DECL
     *     FILE(<unknown>) <-(SOURCE_FILE)- TYPE_DECL
     *     TYPE -(REF)-> TYPE_DECL
     *     TYPE_DECL -(AST)-> *MEMBER ? String[].length ? (TO-DO:)
     *     TYPE_DECL -(AST)-> *MODIFIER ?
     */
    override fun runPass(ts: List<Type>): List<Type> {
        // These should not be null as this pass has already happened prior and should be in cache
        val n = cache.tryGetNamespaceBlock(GLOBAL)!!
        val f = cache.tryGetFile(UNKNOWN)!!
        // Fill up cache
        ts.filterNot { it is RefType }
            .map { Pair(cache.getOrMakeGlobalTypeDecl(it), it) }
            .forEach { (td, st) ->
                logger.debug("Upserting and linking for global type ${st.toQuotedString()}")
                driver.addEdge(n, td, AST)
                driver.addEdge(td, f, SOURCE_FILE)
                driver.addEdge(f, td, CONTAINS)
                cache.getOrMakeGlobalType(st).apply { driver.addEdge(this, td, REF) }
            }
        return ts
    }

}
