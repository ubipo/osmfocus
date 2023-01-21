package net.pfiers.osmfocus.service.osmapi

//internal class ConversionTest {
//    private val nodeId = 40L
//    private val nodeVersion = 30
//    private val nodeLat = 1.0
//    private val nodeLon = 2.0
//    private val nodeTypedId = TypedId(ElementType.NODE, nodeId)
//    private val apiNode = Node(ElementType.NODE, nodeId, nodeVersion, 60, 70, nodeLat, nodeLon)
//    private val node = OsmNode(VersionedIdMeta(nodeId, nodeVersion), emptyMap(), Coordinate(nodeLon, nodeLat))
//
//    private val wayId = 40L
//    private val wayVersion = 30
//    private val wayTypedId = TypedId(ElementType.WAY, wayId)
//    private val apiWay = Way(ElementType.WAY, wayId, wayVersion, 60, 70, listOf(nodeId))
//    private val way = OsmWay(VersionedIdMeta(wayId, wayVersion), emptyMap(), listOf(node))
//
//    private val elements = MutableOsmElements()
//
//    @Test
//    fun toOsmNodeAndAdd() {
//        val actualNode = apiNode.toOsmNodeAndAdd(elements)
//        assertEquals(node, actualNode)
//        assertEquals(hashMapOf(nodeTypedId to node), elements.nodes)
//    }
//
//    @Test
//    fun `toOsmNodeAndAdd with previous stub`() {
//        elements.nodes[nodeTypedId] = OsmNode(nodeId)
//        val actualNode = apiNode.toOsmNodeAndAdd(elements)
//        assertEquals(node, actualNode)
//        assertEquals(hashMapOf(nodeTypedId to node), elements.nodes)
//    }
//
//    @Test
//    fun `toOsmNodeAndAdd with existing`() {
//        elements.nodes[nodeTypedId] = node
//        val addResult = apiNode.toOsmNodeAndAdd(elements)
//        assertNull(addResult)
//        assertEquals(hashMapOf(nodeTypedId to node), elements.nodes)
//    }
//
//    @Test
//    fun toOsmWayAndAdd() {
//        elements.nodes[nodeTypedId] = node
//        val (actualWay, stubNodes) = apiWay.toOsmWayAndAdd(elements)!!
//        assertEquals(way, actualWay)
//        assertEquals(0, stubNodes.size)
//        assertEquals(hashMapOf(wayTypedId to way), elements.ways)
//    }
//
//    @Test
//    fun toOsmRelationAndAdd() {
//    }
//
//    @Test
//    fun splitTypes() {
//    }
//
//    @Test
//    fun toOsmElementsAndAdd() {
//    }
//
//    @Test
//    fun toOsmElements() {
//    }
//}
