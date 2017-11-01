package nismod.transport.network.road;

/**
 * Modified AStarShortestPathFinder from GeoTools to allow for routing in directed graphs.
 * It uses modified version of the AStarIterator (MyAStarIterators) which respects edge directions.
 * If AStar cannot find a path, getPath() returns null as opposed to throwing a WrongPathException.
 * @author Milan Lovric
 *
 */
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Graphable;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.GraphTraversal;
import org.geotools.graph.traverse.GraphWalker;
import org.geotools.graph.traverse.basic.BasicGraphTraversal;
//import org.geotools.graph.traverse.standard.AStarIterator;
//import org.geotools.graph.traverse.standard.AStarIterator.AStarFunctions;
import nismod.transport.network.road.MyAStarIterator.AStarFunctions;

/**
 * Calculates the shortest path between two nodes using the A Star algorithm
 * (for details see http://en.wikipedia.org/wiki/A_star) 
 * @see AStarIterator 
 * @author GermÃ¡n E. Trouillet, Francisco G. MalbrÃ¡n. Universidad Nacional de CÃ³rdoba (UNC)
 *
 *
 *
 * @source $URL$
 */
public class MyAStarShortestPathFinder implements GraphWalker {
        /** Graphs to calculate paths for **/
        private Graph m_graph;

        /** Graph traversal used for the A Star iteration **/
        private GraphTraversal m_traversal;

        /** Underling A Star iterator **/
        private MyAStarIterator m_iterator;

        /**  */
        private Node m_target;

        /**
        * Constructs a new path finder
        *
        * @param graph Graph where we will perform the search.
        * @param source Node to calculate path from.
        * @param target Node to calculate path to.
        * @param afuncs Functions of the A Star.
        */
        public MyAStarShortestPathFinder (
                            Graph graph, Node source, Node target, AStarFunctions afuncs
        ) {
                m_graph = graph;
                m_target = target;
                m_iterator = new MyAStarIterator(source, afuncs);
                m_traversal = new BasicGraphTraversal(graph,this,m_iterator);
        }

        /**
        * Performs the graph traversal and calculates the shortest path from 
        * the source node to destiny node in the graph.
        */
        public void calculate() {
                m_traversal.init();
                m_traversal.traverse(); 
        }


        /**
        *
        * @see GraphWalker#visit(Graphable, GraphTraversal)
        */
        public int visit(Graphable element, GraphTraversal traversal) {
                if (element.equals(m_target)) {
                        return(GraphTraversal.STOP);
                } else {
                        return(GraphTraversal.CONTINUE);
                }
        }

        /**
        * Returns a path <B>from</B> the target <B>to</B> the source. If the desired path is
        * the opposite (from the source to the target), the <i>reverse</i> or the <i>riterator</i> 
        * methods from the <b>Path<b> class can be used.
        *
        * @see Path#riterator()
        * @see Path#reverse()
        *
        * @return A path from the target to the source.
        */
        public Path getPath() throws WrongPathException {
                Path path = new Path();

                path.add(m_target);
                //System.out.println("Adding target " + m_target);
                Node parent = m_iterator.getParent(m_target);
                while ( parent != null ) {
                        path.add(parent);
                        //System.out.println("Adding parent " + parent);
                        parent =  m_iterator.getParent(parent);
                }
                //System.out.printf("Comparing last %d with source %d \n",  path.getLast().getID(), m_iterator.getSource().getID());
                if (!path.getLast().equals(m_iterator.getSource())) {
                //       throw new WrongPathException("getPath: The path obtained doesn't begin correctly");
                	return null;
                }
                return(path);
        }

        /**
        * Does nothing.
        *
        * @see GraphWalker#finish()
        */
        public void finish() {}
}

class WrongPathException extends Exception{
        String message;

        public WrongPathException(String msj){
               message = msj;
        }
        
        public String getMessage(){
        	return message;
        }
}
