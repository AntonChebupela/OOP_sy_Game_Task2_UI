package game.SY.ui.controller;


import net.kurobako.gesturefx.GesturePane.ScrollMode;

import javafx.beans.binding.When;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import game.HelpWithFX.BindFXML;
import game.HelpWithFX.Controller;
import game.SY.ResourceManager;
import game.SY.ResourceManager.ImageResource;
import game.SY.ai.AIPool.VisualiserSurface;
import game.SY.ui.DefaultVisualiserSurface;
import game.SY.ui.Utils;
import game.SY.ui.model.BoardProperty;


@BindFXML("layout/Game.fxml")
public abstract class BaseGame implements Controller {

	@FXML private VBox root;
	@FXML private MenuBar menu;

	@FXML private Menu gameMenu;
	@FXML private MenuItem close;

	@FXML private MenuItem findNode;
	@FXML private MenuItem rules;


	@FXML private MenuItem resetViewport;

	@FXML private CheckMenuItem focusToggle;
	@FXML private CheckMenuItem historyToggle;

	@FXML private CheckMenuItem travelLogToggle;
	@FXML private CheckMenuItem ticketToggle;
	@FXML private CheckMenuItem statusToggle;
	@FXML private CheckMenuItem scrollToggle;
	@FXML private CheckMenuItem animationToggle;

	@FXML private AnchorPane gamePane;
	@FXML private StackPane mapPane;
	@FXML private StackPane setupPane;
	@FXML private StackPane roundsPane;
	@FXML private StackPane ticketsPane;
	@FXML private StackPane playersPane;
	@FXML private StackPane notificationPane;

	@FXML private VBox statusPane;

	private final Stage stage;

	final ResourceManager resourceManager;
	final BoardProperty config;


	final Board board;
	final TravelLog travelLog;
	final TicketsCounter ticketsCounter;
	final Notifications notifications;
	final Status status;

	BaseGame(ResourceManager manager, Stage stage, BoardProperty property) {
		this.resourceManager = manager;
		this.stage = stage;
		this.config = property;
		Controller.bind(this);

		// контролеры
		travelLog = new TravelLog(resourceManager, config);
		ticketsCounter = new TicketsCounter(resourceManager, config);
		notifications = new Notifications(resourceManager, config);
		status = new Status(resourceManager, config);
		board = new Board(resourceManager, notifications, config);
		// пределы видимости
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(gamePane.widthProperty());
		clip.heightProperty().bind(gamePane.heightProperty());
		gamePane.setClip(clip);


		menu.setUseSystemMenuBar(true);


		mapPane.getChildren().add(board.root());
		roundsPane.getChildren().add(travelLog.root());
		playersPane.getChildren().add(ticketsCounter.root());
		notificationPane.getChildren().add(notifications.root());
		statusPane.getChildren().add(status.root());

		close.setOnAction(e -> stage.close());


		findNode.setOnAction(e -> {
			Stage s = new Stage();
			s.setTitle("Find node");
			s.setScene(new Scene(new FindNode(config, s, resourceManager).root()));
			s.show();
		});

		rules.setOnAction(e -> {
			Stage s = new Stage();
			s.setTitle("Rules");
			s.setScene(new Scene(new Rules(s).root()));
			s.show();
		});


		resetViewport.setOnAction(e -> {
			board.resetViewport();
		});



		travelLogToggle.setDisable(true);
		ticketToggle.setDisable(true);

		setAndBind(travelLog.root().visibleProperty(), travelLogToggle.selectedProperty());
		setAndBind(ticketsCounter.root().visibleProperty(), ticketToggle.selectedProperty());
		setAndBind(config.animationProperty(), animationToggle.selectedProperty());
		setAndBind(config.historyProperty(), historyToggle.selectedProperty());
		setAndBind(config.focusPlayerProperty(), focusToggle.selectedProperty());



		scrollToggle.setSelected(config.getScrollMode() == ScrollMode.ZOOM);
		config.scrollModeProperty().bind(new When(scrollToggle.selectedProperty())
				.then(ScrollMode.ZOOM)
				.otherwise(ScrollMode.PAN));

//		if (Platform.getCurrent() == Platform.WINDOWS)
//			scrollToggle.selectedProperty().setValue(false);
//			config.scrollModeProperty().setValue(ScrollMode.ZOOM);

	}

	private <T> void setAndBind(Property<T> source, Property<T> target) {
		target.setValue(source.getValue());
		target.bindBidirectional(source);
	}

	void showOverlay(Node node) {
		setupPane.getChildren().setAll(node);
		showOverlay();
	}

	void showOverlay() {
		gamePane.setEffect(new BoxBlur(5, 5, 2));
		setupPane.setManaged(true);
		setupPane.setVisible(true);
	}

	void hideOverlay() {
		gamePane.setEffect(null);
		setupPane.setManaged(false);
		setupPane.setVisible(false);
	}

	void addStatusNode(Node node) {
		statusPane.getChildren().add(0, node);
	}

	void addMenuItem(MenuItem item) {
		gameMenu.getItems().add(0, item);
	}

	protected ResourceManager manager() {
		return resourceManager;
	}

	@Override
	public Parent root() {
		return root;
	}

	public Stage getStage() {
		return stage;
	}

	VisualiserSurface createVisualiserSurface() {
		return new DefaultVisualiserSurface(stage, board.getVisualiserPane());
	}

}
