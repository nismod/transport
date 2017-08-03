package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
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
		
		if (!super.isValid()) return false;

		ListIterator<DirectedNode> iter = this.listIterator();
		while (iter.hasNext()) {
			DirectedNode node1 = iter.next();
			if (iter.hasNext()) {//there is a further node in the list
				DirectedNode node2 = iter.next();
				if (node1.getOutEdge(node2) == null) return false; //a DIRECTED edge must exist!
				iter.previous();
			}
		}
		return true;
	}
	
	  /**
	   * Internal method for building the edge set of the walk. This method 
	   * calculated the edges upon every call.
	   * 
	   * @return The list of edges for the walk, or null if the edge set could
	   * not be calculated due to an invalid walk.
	   */
	  @Override
	  protected List buildEdges() {
	    ArrayList edges = new ArrayList();
	    for (int i = 1; i < size(); i++) {
	      DirectedNode prev = (DirectedNode)get(i-1);
	      DirectedNode curr = (DirectedNode)get(i);
	      DirectedEdge e = (DirectedEdge) prev.getOutEdge(curr); 
	      if (e != null) edges.add(e);
	      else return(null);  
	    }
	    
	    return(edges);  
	  }
}
