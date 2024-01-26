package game.SY;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Objects;

public class SplashScreen {

    public static void show(Stage primaryStage, Runnable onFinished) {
        Platform.runLater(() -> {
            Stage splashStage = new Stage();
            splashStage.initStyle(StageStyle.UNDECORATED);

            Image logo = new Image(Objects.requireNonNull(SplashScreen.class.getResourceAsStream("/Logo.png")));
            ImageView logoView = new ImageView(logo);
            logoView.setFitWidth(600);
            logoView.setFitHeight(338);

            StackPane root = new StackPane(logoView);
            Scene scene = new Scene(root);
            splashStage.setScene(scene);

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), logoView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), logoView);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                splashStage.close();
                onFinished.run();
            });

            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeIn.play();

            splashStage.show();
        });
    }
}
