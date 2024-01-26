package game.SY.ai;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import game.SY.model.Colour;
import game.SY.model.Player;
import game.SY.model.ScotlandYardGame;
import game.SY.model.ScotlandYardView;
import game.SY.model.Spectator;


public interface PlayerFactory {

	Player createPlayer(Colour colour);


	default List<Spectator> createSpectators(ScotlandYardView view) {
		return Collections.emptyList();
	}


	default void ready(Visualiser visualiser, ResourceProvider provider) {}


	default void finish() {}

}
