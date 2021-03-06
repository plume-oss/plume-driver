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
package io.github.plume.oss.drivers

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.Host
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONObject
import scala.jdk.CollectionConverters
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as un


/**
 * The driver used to connect to a remote Amazon Neptune instance.
 */
class NeptuneDriver internal constructor() : GremlinDriver() {
    private val logger = LogManager.getLogger(NeptuneDriver::class.java)

    private val hostnames = mutableListOf<String>()
    private var port = DEFAULT_PORT
    private var clearOnConnect = false
    private var keyCertChainFile = "src/main/resources/conf/SFSRootCAG2.pem"
    private lateinit var cluster: Cluster
    private val idMapper = mutableMapOf<Long, String>()
    private var id: Long = 0
    private var idStorageLocation = ".${File.separator}"
    private val idFileName = "neptune_ids"

    /**
     * Add one or more the addresses of a Gremlin Servers to the list of servers a Client will try to contact to send
     * requests to. The address should be parseable by InetAddress.getByName(String). That's the only validation
     * performed at this point. No connection to the host is attempted.
     *
     * @param addresses the address(es) of Gremlin Servers to contact.
     */
    fun addHostnames(vararg addresses: String): NeptuneDriver = apply { hostnames.addAll(addresses.toList()) }

    /**
     * Set the port for the Neptune Gremlin server. Default port number is 8182.
     *
     * @param port the port number e.g. 8182
     */
    fun port(port: Int): NeptuneDriver = apply { this.port = port }

    /**
     * Sets the certificate to use by the [Cluster].
     *
     * @param keyChainFile The X.509 certificate chain file in PEM format.
     */
    fun keyCertChainFile(keyChainFile: String): NeptuneDriver = apply { this.keyCertChainFile = keyChainFile }

    /**
     * Will tell the driver to clear the database on connection before reading in existing IDs.
     */
    fun clearOnConnect(clear: Boolean): NeptuneDriver = apply { clearOnConnect = clear }

    /**
     * Where the driver will store the mapped IDs. This must be an existing directory.
     */
    fun idStorageLocation(location: String): NeptuneDriver = apply {
        File(location).let { f ->
            require(f.isDirectory && f.exists()) { "The storage location should be an existing directory." }
        }
        idStorageLocation = if (!location.endsWith(File.separator)) "$location${File.separator}"
        else location
    }

    /**
     * Connects to the graph database with the given configuration.
     * See [Amazon Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-gremlin-java.html).
     *
     * @throws IllegalArgumentException if the graph database is already connected.
     */
    @Suppress("DEPRECATION")
    override fun connect(): NeptuneDriver = apply {
        PlumeTimer.measure(DriverTimeKey.CONNECT_DESERIALIZE) {
            require(!connected) { "Please close the graph before trying to make another connection." }
            cluster = Cluster.build().addContactPoints(*hostnames.toTypedArray())
                .port(port)
                .enableSsl(true)
                .keyCertChainFile(keyCertChainFile)
                .create()
            logger.info("Connected to the following Neptune cluster: $hostnames")
            super.g = traversal().withRemote(DriverRemoteConnection.using(cluster))
            graph = g.graph
            connected = true
        }
        if (clearOnConnect) clearGraph()
        else deserializeIds()
    }

    private fun resetIdMapper() {
        idMapper.clear()
        idMapper[-1L] = "null"
        id = 0
    }

    private fun serializeIds() {
        val filePath = "$idStorageLocation$idFileName"
        File(filePath).let { f ->
            if (!f.exists())
                f.createNewFile()
            else
                File("$idStorageLocation${File.separator}$idFileName").delete()
        }
        FileWriter(filePath).use { fw ->
            idMapper.forEach { (l, s) -> fw.write("$l:$s\n") }
        }
        logger.info("Serialized ${idMapper.size} IDs to disk.")
    }

    private fun deserializeIds() {
        val filePath = "$idStorageLocation$idFileName"
        if (!File(filePath).exists()) {
            populateIdMapper()
        } else {
            FileReader(filePath).useLines { ls ->
                ls.forEach { l ->
                    val split = l.trim().split(":")
                    if (split.size == 2) idMapper[split[0].toLong()] = split[1].trim()
                }
            }
            logger.info("Deserialized ${idMapper.size} IDs from disk.")
        }
        id = (idMapper.keys.maxOrNull() ?: -1L) + 1
    }

    /**
     * When connecting to a database with a subgraph already loaded, create a mapping for existing graph data.
     */
    private fun populateIdMapper() {
        logger.info("Populating ID mapper from database.")
        resetIdMapper()
        File("$idStorageLocation${File.separator}$idFileName").delete()
        val vCount = g.V().count().next()
        var inc = 0L
        val loadedIds = idMapper.values.toSet()
        (1..vCount).chunked(5000).map { Pair(it.minOrNull() ?: 0L, it.maxOrNull() ?: 10000) }
            .flatMap { (l, h) -> g.V().order().by(T.id, Order.asc).range(l, h).id().toList().map { it.toString() } }
            .filterNot(loadedIds::contains)
            .forEach { id -> idMapper[inc++] = id }
        id = (idMapper.keys.maxOrNull() ?: -1L) + 1
    }

    /**
     * Attempts to close the graph database connection and resources.
     *
     * @throws IllegalArgumentException if one attempts to close an already closed graph.
     */
    override fun close() {
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) {
            require(connected) { "Cannot close a graph that is not already connected!" }
            try {
                cluster.close()
                g.close()
            } catch (e: Exception) {
                logger.warn("Exception thrown while attempting to close graph.", e)
            } finally {
                // Have to also clear the cache otherwise the IDs won't be mapped correctly
                connected = false
                serializeIds()
                resetIdMapper()
            }
        }
    }

    override fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> {
        var result: GraphTraversal<Vertex, Vertex>? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val strId = idMapper[v.id()]
            result = if (strId != null) g.V(strId)
            else g.V("null")
        }
        return result!!
    }

    /**
     * Given a [NewNodeBuilder], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [NewNodeBuilder] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createVertex(v: NewNodeBuilder): Vertex {
        val propertyMap = prepareVertexProperties(v)
        var traversalPointer = g.addV(v.build().label())
        for ((key, value) in propertyMap) traversalPointer = traversalPointer.property(key, value)
        return traversalPointer.next().apply {
            idMapper[++id] = this.id().toString()
            v.id(id)
        }
    }

    override fun deleteVertex(id: Long, label: String?) {
        val mappedId = idMapper[id]
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            res = if (mappedId != null) g.V(mappedId).hasNext()
            else false
        }
        if (!res) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V(mappedId).drop().iterate() }
        idMapper.remove(id)
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            val srcId = idMapper[src.id()]
            val dstId = idMapper[tgt.id()]
            g.V(srcId).outE(edge).where(un.otherV().hasId(dstId)).drop().iterate()
        }
    }

    // This handles Neptune -> ODB
    override fun mapVertexKeys(props: Map<Any, Any>): Map<String, Any> {
        val outM = mutableMapOf<String, Any>()
        props.filterKeys { it != T.id }.mapKeys { it.key.toString() }.toMap(outM)
        val id = props.getOrDefault(T.id, "null")
        idMapper.entries.find { it.value == id }?.let { idL -> outM["id"] = idL.key }
        return outM
    }

    // This handles ODB -> Neptune
    override fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> {
        val outMap = VertexMapper.prepareListsInMap(
            VertexMapper.stripUnusedProperties(
                v.build().label(),
                CollectionConverters.MapHasAsJava(v.build().properties()).asJava().toMutableMap()
            )
        ).toMutableMap()
        if (outMap.containsKey("id")) {
            outMap["id"] = idMapper[outMap["id"]]!!
        }
        return outMap
    }

    override fun assignId(n: NewNodeBuilder, v: Vertex) = n.apply {
        idMapper[++id] = v.id().toString()
        this.id(id)
    }

    override fun bulkAddNodes(vs: List<NewNodeBuilder>) {
        if (vs.isEmpty()) return
        var gPtr: GraphTraversal<*, *>? = null
        vs.forEach { v ->
            if (!exists(v)) {
                PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
                    if (gPtr == null) gPtr = g.addV(v.build().label()).property(T.id, idMapper[v.id()])
                    else gPtr?.addV(v.build().label())?.property(T.id, idMapper[v.id()])
                    prepareVertexProperties(v).forEach { (k, v) -> gPtr?.property(k, v) }
                }
            }
        }
        gPtr?.next()
    }

    override fun bulkAddEdges(es: List<DeltaGraph.EdgeAdd>) {
        if (es.isEmpty()) return
        var gPtr: GraphTraversal<*, *>? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            es.map { Triple(g.V(idMapper[it.src.id()]).next(), it.e, g.V(idMapper[it.dst.id()]).next()) }
                .forEach { (src, e, dst) ->
                    if (gPtr == null) gPtr = g.V(src).addE(e).to(dst)
                    else gPtr?.V(src)?.addE(e)?.to(dst)
                }
        }
        gPtr?.next()
    }

    override fun bulkTxReads(
        dg: DeltaGraph,
        vAdds: MutableList<NewNodeBuilder>,
        eAdds: MutableList<DeltaGraph.EdgeAdd>,
        vDels: MutableList<DeltaGraph.VertexDelete>,
        eDels: MutableList<DeltaGraph.EdgeDelete>,
    ) {
        val temp = mutableListOf<NewNodeBuilder>()
        dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }
            .filterNot(::exists)
            .forEachIndexed { i, va ->
                if (temp.none { va === it }) temp.add(va.id(-(i + 1).toLong()))
            }
        temp.map {
            if (it.id() < 0) {
                it.id(++id)
                idMapper[id] = UUID.randomUUID().toString()
            }
            it
        }.toCollection(vAdds)
        dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().distinct().filterNot { exists(it.src, it.dst, it.e) }
            .toCollection(eAdds)
        dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { g.V(idMapper[it.id]).hasNext() }
            .toCollection(vDels)
        dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
            .toCollection(eDels)
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { res = g.V(idMapper[id]).hasNext() }
        if (!res) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V(idMapper[id]).property(key, value).iterate() }
    }

    override fun getWholeGraph(): overflowdb.Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            g.V().valueMap<Any>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens).toList()
                .map { VertexMapper.mapToVertex(mapVertexKeys(it)) }
                .forEach { addNodeToODB(graph, it) }
            g.E().toList()
                .map { e ->
                    Triple(
                        graph.node(idMapper.entries.find { it.value == e.outVertex().id() }!!.key),
                        graph.node(idMapper.entries.find { it.value == e.inVertex().id() }!!.key),
                        e.label()
                    )
                }
                .forEach { (src, dst, e) ->
                    src?.addEdge(e, dst)
                }
        }
        return graph
    }

    override fun clearGraph() = apply {
        resetIdMapper()
        var noVs = 0L
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            noVs = g.V().count().next()
        }
        if (noVs < 10000) {
            // On small graphs, we can get away with using Gremlin to clear the graph
            PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
                var deleted = 0L
                val step = 100
                while (deleted < noVs) {
                    g.V().sample(step).drop().iterate()
                    deleted += step
                }
            }
        } else {
            val objectMapper = ObjectMapper()
            val contentType = mapOf("Content-Type" to "application/json")
            // On larger graphs, it's better to do a database reset
            cluster.allHosts().mapNotNull { host: Host ->
                val hostname = host.address.hostString
                val endpoint = "https://$hostname:8182/system"
                runCatching {
                    val payload = khttp.post(
                        url = endpoint,
                        headers = contentType,
                        data = objectMapper.writeValueAsString(
                            mapOf("action" to "initiateDatabaseReset")
                        )
                    ).jsonObject["payload"] as JSONObject
                    Pair(endpoint, payload)
                }.onFailure { logger.warn("Unable to initiate database reset for $endpoint.", it) }
                    .getOrNull()
            }.forEach { (endpoint, payload) ->
                runCatching {
                    val token = payload["token"]
                    khttp.post(
                        url = endpoint,
                        headers = contentType,
                        data = objectMapper.writeValueAsString(
                            mapOf(
                                "action" to "performDatabaseReset",
                                "token" to token
                            )
                        )
                    )
                }.onFailure { logger.warn("Unable to perform database reset for $endpoint.", it) }
            }
            // Wait until all hosts are healthy
            var healthy = false
            while (!healthy) {
                Thread.sleep(30000)
                healthy = cluster.allHosts().mapNotNull { host: Host ->
                    val hostname = host.address.hostString
                    val endpoint = "https://$hostname:8182/status"
                    runCatching {
                        val payload = khttp.get(
                            url = endpoint,
                            headers = contentType
                        ).jsonObject["status"] as String
                        payload == "healthy"
                    }.onFailure { logger.warn("Unable to get health of $endpoint.", it) }
                        .getOrNull() ?: false
                }.fold(true) { x, y -> x && y }
            }
        }
    }

    companion object {
        /**
         * Default port number a remote Gremlin server.
         */
        const val DEFAULT_PORT = 8182
    }
}