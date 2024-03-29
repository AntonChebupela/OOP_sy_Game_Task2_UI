package game.SY.ui.controller;

import game.SY.ui.model.PlayerProperty;
import net.kurobako.gesturefx.GesturePane;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;
import game.HelpWithFX.BindFXML;
import game.HelpWithFX.Controller;
import game.HelpWithFX.interpolator.DecelerateInterpolator;
import game.SY.ResourceManager;
import game.SY.ResourceManager.ImageResource;
import game.SY.model.Colour;
import game.SY.model.DoubleMove;
import game.SY.model.Move;
import game.SY.model.MoveVisitor;
import game.SY.model.Player;
import game.SY.model.ScotlandYardView;
import game.SY.model.TicketMove;
import game.SY.ui.GameControl;
import game.SY.ui.ModelConfiguration;
import game.SY.ui.model.BoardProperty;

import static io.atlassian.fugue.Option.some;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;


@BindFXML("layout/Map.fxml")
public final class Board implements Controller, GameControl, Player {

	private static final Duration DURATION = Duration.millis(400);

	@FXML private Pane root;
	@FXML private ImageView mapView;
	@FXML private Pane historyPane;
	@FXML private Pane visualiserPane;
	@FXML private Pane cuePane;
	@FXML private Pane counterPane;
	@FXML private Pane hintPane;

	private final Notifications notifications;
	private final BoardProperty property;
	private final GesturePane gesturePane;
	private final ResourceManager manager;

	private final Map<Colour, Counter> counters = new HashMap<>();
	private final Map<Colour, BoardPlayer> players = new HashMap<>();
	private final Map<Integer, MoveHint> hints = new HashMap<>();
	private final Map<Colour, Path> paths = new HashMap<>();

	private ModelConfiguration configuration;

	Board(ResourceManager manager, Notifications notifications, BoardProperty property) {
		Controller.bind(this);
		this.manager = requireNonNull(manager);
		this.notifications = requireNonNull(notifications);
		this.property = requireNonNull(property);

		gesturePane = new GesturePane(root);
		gesturePane.setMinScale(Double.NEGATIVE_INFINITY);
		gesturePane.scrollModeProperty().bind(property.scrollModeProperty());
		historyPane.visibleProperty().bind(property.historyProperty());
		Image image = manager.getImage(ImageResource.MAP);
		mapView.setImage(image);
		lockSize(image.getWidth(), image.getHeight(), root, visualiserPane, historyPane);
		Platform.runLater(() -> gesturePane.zoomTo(0, Point2D.ZERO));
	}

	private static void lockSize(double width, double height, Region... regions) {
		for (Region region : regions){
			region.setPrefSize(width, height);
			region.setMaxSize(width, height);
			region.setMinSize(width, height);
//			region.resize(width, height);
		}
	}

	void setBoardPlayer(Colour colour, BoardPlayer player) {
		this.players.put(colour, player);
	}

	@Override
	public void onGameAttach(ScotlandYardView view, ModelConfiguration configuration) {
		this.configuration = requireNonNull(configuration);
		unlock();

		for (PlayerProperty property : configuration.players()) {


			Counter counter = new Counter(
					manager,
					this.property.animationProperty(),
					property.colour(),
					property.location());
			this.counters.put(property.colour(), counter);
			counterPane.getChildren().add(counter.root());


			Path path = new Path();
			path.setFill(Color.TRANSPARENT);
			path.setStroke(Color.valueOf(property.colour().name()));
			path.setStrokeWidth(30d);
			path.setOpacity(0.5);
			historyPane.getChildren().add(path);
			paths.put(property.colour(), path);

			view.getPlayerLocation(property.colour())
					.filter(l -> l != 0)
					.ifPresent(location -> {
						Point2D d = coordinateAtNode(location);
						path.getElements().add(new MoveTo(d.getX(), d.getY()));
					});
		}

	}

	@Override
	public void onGameDetached() {
		clearMoveHints();
		clearActionCues();
		counters.clear();
		counterPane.getChildren().clear();
		paths.clear();
		historyPane.getChildren().clear();
		lock();
	}

	MoveHint hintAt(int node) {
		return hints.get(node);
	}

	private void drawMoveHints(Set<Move> moves, Consumer<Move> moveCallback) {
		clearMoveHints();
		Function<Integer, MoveHint> mapping = location -> new MoveHint(manager,
				this,
				location,
				moveCallback);
		for (Move move : moves) {
			move.visit(new MoveVisitor() {
				@Override
				public void visit(TicketMove move) {
					hints.computeIfAbsent(move.destination(), mapping).addMove(move);
				}

				@Override
				public void visit(DoubleMove move) {
					hints.computeIfAbsent(move.firstMove().destination(), mapping);
					hints.computeIfAbsent(move.secondMove().destination(), mapping).addMove(move);
				}
			});
		}
		hints.values().stream().map(MoveHint::root)
				.forEach(n -> hintPane.getChildren().add(n));
	}

	private void clearMoveHints() {
		hints.values().forEach(MoveHint::discard);
		hints.clear();
		hintPane.getChildren().clear();
	}

	@Override
	public void onMoveMade(ScotlandYardView view, Move move) {
		Counter counter = counters.get(move.colour());
		move.visit(new MoveVisitor() {
			@Override
			public void visit(TicketMove move) {
				counter.animateTicketMove(move, some(() -> {
					counter.location(move.destination());
					counter.updateLocation();
				}));
				drawHistory(move.destination(), move.colour());
			}

			@Override
			public void visit(DoubleMove move) {
				counter.animateTicketMove(move.firstMove(),
						some(() -> {
							counter.location(move.finalDestination());
							counter.animateTicketMove(
									move.secondMove(),
									some(() -> {
										counter.location(move.finalDestination());
										counter.updateLocation();
									}));
						}));
				drawHistory(move.firstMove().destination(), move.colour());
				drawHistory(move.secondMove().destination(), move.colour());
			}
		});
	}

	private void drawHistory(int location, Colour colour) {
		if (location == 0) {
			return;
		}
		Point2D end = coordinateAtNode(location);
		ObservableList<PathElement> elements = paths.get(colour).getElements();
		if (elements.isEmpty()) {
			elements.add(new MoveTo(end.getX(), end.getY()));
		} else {
			elements.add(new LineTo(end.getX(), end.getY()));
		}
	}

	private void showActionCueAtNode(int node) {

		Point2D point = coordinateAtNode(node);
		Circle circle = new Circle();
		circle.setRadius(10);
		circle.setFill(Color.YELLOW);
		cuePane.getChildren().add(circle);
		circle.setTranslateX(point.getX() - circle.getRadius());
		circle.setTranslateY(point.getY() - circle.getRadius());
		circle.setOpacity(0.5);

		Duration duration = Duration.millis(1000);
		ScaleTransition st = new ScaleTransition(duration);
		st.setToX(10);
		st.setToY(10);
		FadeTransition ft = new FadeTransition(duration);
		ft.setToValue(0);

		ParallelTransition pt = new ParallelTransition(st, ft);
		pt.setInterpolator(new DecelerateInterpolator(2));
		pt.setNode(circle);
		pt.setCycleCount(Animation.INDEFINITE);
		pt.play();
	}

	private void clearActionCues() {
		cuePane.getChildren().clear();
	}

	Point2D coordinateAtNode(int node) {
		return manager.coordinateAtNode(node);
	}

	@Override
	public Parent root() {
		return gesturePane;
	}

	Pane getVisualiserPane() {
		return visualiserPane;
	}

	private void focusOnNode(int location) {
		if (location == 0) return;
		gesturePane.animate(DURATION)
				.interpolateWith(DecelerateInterpolator.DEFAULT)
				.centreOn(coordinateAtNode(location));
	}

	void resetViewport() {
		gesturePane.animate(DURATION)
				.interpolateWith(DecelerateInterpolator.DEFAULT)
				.zoomTo(0, gesturePane.targetPointAtViewportCentre());
	}

	void lock() {
		asList(cuePane, hintPane).forEach(p -> p.setVisible(false));
	}

	private void unlock() {
		asList(cuePane, hintPane).forEach(p -> p.setVisible(true));
	}

	@Override
	public void makeMove(ScotlandYardView view,
	                     int location,
	                     Set<Move> moves,
	                     Consumer<Move> callback) {
		Platform.runLater(() -> {
			Colour colour = resolveColour(moves);
			BoardPlayer player = players.get(colour);
			Counter counter = counters.get(colour);
			if (player == null)
				throw new IllegalStateException(
						"Player " + colour + " has no associated BoardPlayer");
			counter.location(location);

			counter.location().ifPresent(this::showActionCueAtNode);

			if (property.focusPlayerProperty().get()) focusOnNode(location);
			player.makeMove(
					new CurrentHintedBoard(),
					view,
					location,
					moves,
					move -> {
						clearActionCues();
						view.getPlayerLocation(colour).ifPresent(counter::location);
						callback.accept(move);
					});
		});

	}

	private static Colour resolveColour(Set<Move> moves) {
		if (moves.isEmpty()) throw new IllegalStateException("Cannot resolve empty moves");
		return moves.iterator().next().colour();
	}

	interface BoardPlayer {

		void makeMove(HintedBoard board,
		              ScotlandYardView view,
		              int location,
		              Set<Move> moves,
		              Consumer<Move> callback);

	}

	private class CurrentHintedBoard implements HintedBoard {

		@Override
		public Notifications notifications() {
			return notifications;
		}

		@Override
		public ModelConfiguration configuration() {
			return configuration;
		}

		@Override
		public void showMoveHints(Set<Move> moves, Consumer<Move> callback) {
			drawMoveHints(moves, callback);
		}

		@Override
		public void hideMoveHints() {
			clearMoveHints();
		}

		@Override
		public void scrollToLocation(int location) {
			if (location == 0) return;
			gesturePane.animate(DURATION)
					.interpolateWith(DecelerateInterpolator.DEFAULT)
					.centreOn(coordinateAtNode(location));
		}
	}

	interface HintedBoard {

		Notifications notifications();

		ModelConfiguration configuration();

		void showMoveHints(Set<Move> moves, Consumer<Move> callback);

		void hideMoveHints();

		void scrollToLocation(int location);
	}

}
