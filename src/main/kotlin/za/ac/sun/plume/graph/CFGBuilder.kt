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
package za.ac.sun.plume.graph

import org.apache.logging.log4j.LogManager
import soot.SootClass
import soot.SootMethod
import soot.Unit
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.drivers.IDriver

class CFGBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(CFGBuilder::javaClass)

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, unitToVertex: MutableMap<Unit, PlumeVertex>) {
        logger.debug("Building CFG for ${mtd.declaration}")
    }

}