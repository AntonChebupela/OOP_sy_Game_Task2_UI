package game.SY;

import game.SY.ui.Utils;
import game.SY.ui.controller.LocalGame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public final class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		Thread.currentThread().setUncaughtExceptionHandler(
				(thread, throwable) -> Utils.handleFatalException(throwable));

		primaryStage.setTitle("Scotland Yard");


		SplashScreen.show(primaryStage, () -> {
			Platform.runLater(() -> LocalGame.newGame(Utils.setupResources(), primaryStage, false));
		});
	}
}
