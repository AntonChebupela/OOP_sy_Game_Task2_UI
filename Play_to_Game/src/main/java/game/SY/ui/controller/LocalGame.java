package game.SY.ui.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import game.SY.HelpWithTests.CodeGenRecorder;
import game.SY.HelpWithTests.GameModelSequencePUMLCodeGen;
import game.SY.HelpWithTests.PlayOutTestCodeGen;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import game.SY.ResourceManager;
import game.SY.ResourceManager.ImageResource;
import game.SY.ai.AIPool;
import game.SY.model.Colour;
import game.SY.model.PlayerConfiguration;
import game.SY.model.ScotlandYardGame;
import game.SY.model.ScotlandYardModel;
import game.SY.model.ScotlandYardView;
import game.SY.model.Spectator;
import game.SY.ui.GameControl;
import game.SY.ui.Utils;
import game.SY.ui.model.BoardProperty;
import game.SY.ui.model.ModelProperty;
import game.SY.ui.model.PlayerProperty;
import game.SY.ui.model.Side;

import static io.atlassian.fugue.Option.fromOptional;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static game.SY.ui.Utils.handleFatalException;

public final class LocalGame extends BaseGame implements Spectator {

	private final boolean showCapturedTest;

	public static void newGame(ResourceManager manager, Stage stage, boolean captureTest) {
		BaseGame controller = new LocalGame(manager, stage, captureTest);
		stage.setTitle("ScotlandYard" + (captureTest ? "(test capture mode)" : ""));
		stage.setScene(new Scene(controller.root()));
		stage.getIcons().add(manager.getImage(ImageResource.ICON));
		stage.show();
	}

	private LocalGame(ResourceManager manager, Stage stage, boolean showCapturedTest) {
		super(manager, stage, new BoardProperty());
		this.showCapturedTest = showCapturedTest;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		MenuItem newGame = new MenuItem("New game");
		MenuItem showTests = new MenuItem("Restart in test capture mode");
		newGame.setOnAction(e -> LocalGame.newGame(resourceManager, new Stage(), false));
		showTests.setOnAction(e -> {
			getStage().close();
			LocalGame.newGame(resourceManager, new Stage(), true);
		});
		addMenuItem(newGame);
		setupGame();
	}

	private void setupGame() {
		StartScreen startScreen = new StartScreen(resourceManager, config, this::createGame);
		showOverlay(startScreen.root());
	}

	private void createGame(ModelProperty setup) {
		hideOverlay();
		try {
			Game game = new Game(setup);
		} catch (Exception e) {
			e.printStackTrace();
			handleFatalException(e);
		}

	}

	private class Game implements GameControl {

		private static final String NOTIFY_GAMEOVER = "notify_gameover";
		private final ModelProperty setup;
		private final ScotlandYardGame model;
		private final List<GameControl> controls;
		private final AIPool<Side> pool = new AIPool<>(
				createVisualiserSurface(),
				Utils::handleFatalException);
		private final CodeGenRecorder recorder = new CodeGenRecorder(ImmutableList.of(
				new PlayOutTestCodeGen(),
				new GameModelSequencePUMLCodeGen()));

		Game(ModelProperty setup) throws Exception {
			this.setup = setup;

			List<PlayerProperty> joining = setup.players();

			//  AI pool
			for (PlayerProperty property : joining) {
				property.ai().ifPresent(ai -> pool.addToGroup(
						property.side(),
						property.colour(),
						ai));
			}

			List<PlayerConfiguration> configs = joining.stream()
					.map(p -> new PlayerConfiguration.Builder(p.colour())
							.at(p.location())
							.with(p.ticketsAsMap())
							.using(board)
							.build())
					.map(pc -> recorder.observePlayer(pc))
					.collect(Collectors.toList());

			PlayerConfiguration mrX = configs.stream()
					.filter(p -> p.colour.isMrX())
					.findFirst()
					.orElseThrow(AssertionError::new);

			List<PlayerConfiguration> detectives = configs.stream()
					.filter(p -> p.colour.isDetective())
					.collect(toList());

			model = new ScotlandYardModel(
					setup.revealRounds(),
					setup.graphProperty().get(),
					mrX,
					detectives.get(0),
					detectives.stream().skip(1).toArray(PlayerConfiguration[]::new));


			recorder.snap(model);

			controls = asList(
					board,
					travelLog,
					ticketsCounter,
					status,
					this);

			pool.initialise(resourceManager, model);
			// Add all players to board
			for (PlayerProperty property : joining) {
				board.setBoardPlayer(property.colour(),
						BoardPlayers.resolve(
								fromOptional(pool.createPlayer(property.colour())),
								fromOptional(property.name()),
								() -> onGameOver(model, model.getCurrentPlayer().isDetective()
										? ImmutableSet.of(Colour.BLACK)
										: ImmutableSet.copyOf(stream(Colour.values())
										.filter(Colour::isDetective)
										.collect(toList())))));
			}

			model.registerSpectator(recorder.createSpectator());
			controls.forEach(model::registerSpectator);
			controls.forEach(l -> l.onGameAttach(model, setup));
			model.startRotate();
		}

		void terminate() {
			controls.forEach(model::unregisterSpectator);
			controls.forEach(GameControl::onGameDetached);
			pool.terminate();
		}

		@Override
		public void onRotationComplete(ScotlandYardView view) {
			if (!view.isGameOver()) model.startRotate();
		}

		@Override
		public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
			Platform.runLater(() -> {
				board.lock();
				notifications.dismissAll();
				Notifications.NotificationBuilder.Notification gameOver = new Notifications.NotificationBuilder(
						"Game over, winner is " + winningPlayers)
						.addAction("Start again(same location)", () -> {
							notifications.dismissAll();
							terminate();
							createGame(setup);
						})
						.addAction("Main menu", () -> {
							notifications.dismissAll();
							terminate();
							setupGame();
						}).create();
				notifications.show(NOTIFY_GAMEOVER, gameOver);
				if (showCapturedTest) {
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle("Test DSL capture");
					alert.setHeaderText("Test captured successfully");

					Map<CodeGenRecorder.CodeGen, String> map = recorder.readOut("defaultGraph()");

					List<Tab> tabs = map.entrySet().stream()
							.sorted(Comparator.comparing(e -> e.getKey().name()))
							.map(e -> {
								Tab tab = new Tab(e.getKey().name());
								tab.setContent(mkCodePane(e.getValue()));
								return tab;
							}).collect(toList());
					TabPane pane = new TabPane();
					pane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
					pane.getTabs().setAll(tabs);
					pane.setPrefSize(700, 500);
					alert.getDialogPane().setContent(pane);
					alert.setResizable(true);
					alert.showAndWait();
				}
			});
		}

		private Node mkCodePane(String value) {
			TextArea textArea = new TextArea(value);
			textArea.setStyle("-fx-font-family: monospace");
			textArea.setEditable(false);
			textArea.setWrapText(false);
			return textArea;
		}

	}

}
