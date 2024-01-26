package game.SY.ui.controller;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import game.HelpWithFX.BindFXML;
import game.HelpWithFX.Controller;
import game.SY.ResourceManager;
import game.SY.ai.AI;
import game.SY.ui.model.BoardProperty;
import game.SY.ui.model.ModelProperty;

@BindFXML(value = "layout/StartScreen.fxml", css = "style/startscreen.css")
public final class StartScreen implements Controller {

	@FXML private VBox root;
	@FXML private Tab gameSetup;
	@FXML private Tab savedConfigs;
	@FXML private Tab savedGames;
	@FXML private Button start;

	private final ResourceManager manager;
	private final BoardProperty config;

	StartScreen(ResourceManager manager, BoardProperty config,
	            Consumer<ModelProperty> consumer) {
		this.manager = manager;
		this.config = config;
		Controller.bind(this);

		ArrayList<AI> ais = new ArrayList<>(AI.scanClasspath());

		ais.add(0, null);

		GameSetup setupController = new GameSetup(this.manager, this.config,
				ModelProperty.createDefault(manager), ais, EnumSet.allOf(GameSetup.Features.class));

		gameSetup.setContent(setupController.root());


		savedConfigs.setDisable(true);
		// savedConfigs.setContent(new SavedConfigsController(consumer).root());
		savedGames.setDisable(true);
		// savedGames.setContent(new SavedGamesController(consumer).root());

		start.disableProperty().bind(setupController.readyProperty().not());
		start.setOnAction(e -> {
			ModelProperty property = setupController.createGameConfig();
			consumer.accept(property);
		});

	}

	@Override
	public Parent root() {
		return root;
	}
}
