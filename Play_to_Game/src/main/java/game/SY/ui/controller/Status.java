package game.SY.ui.controller;

import java.util.Set;

import game.SY.model.Colour;
import game.SY.model.Move;
import game.SY.model.ScotlandYardView;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import game.HelpWithFX.BindFXML;
import game.HelpWithFX.Controller;
import game.SY.ResourceManager;
import game.SY.ui.GameControl;
import game.SY.ui.ModelConfiguration;
import game.SY.ui.model.BoardProperty;

@BindFXML("layout/Status.fxml")
public final class Status implements Controller, GameControl {

	@FXML private ToolBar root;
	@FXML private Label round;
	@FXML private Label player;
	@FXML private Label time;
	@FXML private Label status;
	@FXML private Slider volume;

	private final ResourceManager manager;

	Status(ResourceManager manager, BoardProperty config) {
		Controller.bind(this);
		this.manager = manager;
	}

	@Override
	public void onGameAttach(ScotlandYardView view, ModelConfiguration configuration) {
		bindView(view);
	}

	@Override
	public void onRoundStarted(ScotlandYardView view, int round) {
		bindView(view);
	}

	@Override
	public void onMoveMade(ScotlandYardView view, Move move) {
		bindView(view);
	}

	private void bindView(ScotlandYardView view) {
		int round = view.getCurrentRound();
		this.round.setText(round == 0 ? "N/A" : round + " of " + view.getRounds().size());
		this.player.setText(view.getCurrentPlayer().toString());
		this.status.setText(String.format("Waiting move(%s)", view.getCurrentPlayer()));
	}

	@Override
	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
		status.setText("Game completed, winning player:" + view.getWinningPlayers());
	}

	@Override
	public Parent root() {
		return root;
	}
}
