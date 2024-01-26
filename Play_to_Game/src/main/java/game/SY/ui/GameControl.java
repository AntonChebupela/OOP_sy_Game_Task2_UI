package game.SY.ui;

import game.SY.model.ScotlandYardView;
import game.SY.model.Spectator;



public interface GameControl extends Spectator {

	default void onGameAttach(ScotlandYardView view, ModelConfiguration configuration) {}

	default void onGameDetached() {}

}
