package game.SY.ai;

import game.SY.model.Ticket;
import game.SY.model.Transport;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import game.GameKT.graph.Graph;


public interface ResourceProvider {


	Image getMap();


	Image getTicket(Ticket ticket);


	Graph<Integer, Transport> getGraph();


	Point2D coordinateAtNode(int node);

}
