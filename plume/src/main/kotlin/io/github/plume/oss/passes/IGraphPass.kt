/*
 * Copyright 2020 David Baker Effendi
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
package io.github.plume.oss.passes

import soot.SootClass
import soot.toolkits.graph.BriefUnitGraph

/**
 * A builder that builds CPG method bodies from [BriefUnitGraph] objects.
 */
interface IMethodPass {
    /**
     * Builds the method body.
     *
     * @param graph The [BriefUnitGraph] of a method body to build the graph off of.
     */
    fun runPass(graph: BriefUnitGraph): BriefUnitGraph
}

/**
 * A builder that builds CPG program structure from [SootClass] objects.
 */
interface IProgramStructurePass {
    /**
     * Builds the program structure.
     *
     * @param cs The list of [SootClass] to build the graph off of.
     */
    fun runPass(cs: List<SootClass>): List<SootClass>
}

/**
 * A builder that builds CPG type sub-graphs from [soot.Type] objects.
 */
interface ITypePass {
    /**
     * Builds the program type structure.
     *
     * @param ts The list of [soot.Type] to build the graph off of.
     */
    fun runPass(ts: List<soot.Type>): List<soot.Type>
}