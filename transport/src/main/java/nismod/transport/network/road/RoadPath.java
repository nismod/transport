package nismod.transport.network.road;

import java.util.Collection;
import java.util.ListIterator;

import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Node;

/**
 * A directed path (a list of directed nodes)
 * @author Milan Lovric
 *
 */
public class RoadPath extends Path {

	public RoadPath() {
		// TODO Auto-generated constructor stub
	}

	public RoadPath(Collection nodes) {
		super(nodes);
		// TODO Auto-generated constructor stub
	}
	
	/* (non-Javadoc)
	 * @see org.geotools.graph.path.Path#isValid()
	 * Check if there is a directed edge between two adjacent nodes.
	 * A valid road path is one in which each pair of adjacent nodes in the sequence 
	 * has a directed edge between them, in the direction of their order.
	 */
	@Override
	public boolean isValid() {
		ListIterator<DirectedNode> iter = this.listIterator();
		while (iter.hasNext()) {
			DirectedNode node1 = iter.next();
			if (iter.hasNext()) {//there is a further node in the list
				DirectedNode node2 = iter.next();
				if (node1.getOutEdge(node2) == null) return false; //a DIRECTED edge must exist!
			}
		}
		return true;
	}
}