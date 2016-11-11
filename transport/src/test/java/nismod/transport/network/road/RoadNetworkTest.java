/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Tests for the RoadNetwork class
 * @author Milan Lovric
 *
 */
public class RoadNetworkTest {
	
	public static void main( String[] args ) throws IOException	{
				
		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		
		//visualise the shapefiles
		roadNetwork.visualise();
	}

	@Test
	public void test() throws IOException {
		
		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		DirectedGraph rn = roadNetwork.getNetwork();
		
		Iterator iter = rn.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 55) nodeA = node;
			if (node.getID() == 40) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 55, nodeA.getID());
		assertEquals("Node ID is correct", 40, nodeB.getID());
		DirectedEdge edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		DirectedEdge edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		SimpleFeature sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "W", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "E", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));
		
		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 6, nodeB.getDegree());
		assertEquals("Node out degree is correct", 3, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 3, nodeB.getInDegree());
		
		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 87) nodeA = node;
			if (node.getID() == 105) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 87, nodeA.getID());
		assertEquals("Node ID is correct", 105, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		
		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());
		
		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 95) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 95, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48513L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "N", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);
		
		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());
		
		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		assertNull("Expecting no edge in this direction", edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48317L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		
		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 2, nodeA.getDegree());
		assertEquals("Node out degree is correct", 1, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 1, nodeA.getInDegree());
		
		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 95) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 95, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48456L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.1, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);
	}
}
