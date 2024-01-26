package game.SY.ui.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import game.HelpWithFX.BindFXML;
import game.HelpWithFX.Controller;

@BindFXML("layout/Rules.fxml")
public final class Rules implements Controller {

	@FXML private VBox root;
	@FXML private TextArea content;
	@FXML private Button dismiss;

	Rules(Stage stage) {
		Controller.bind(this);
		try {
			String rules = Resources.toString(getClass().getResource("/Rules.txt"),
					StandardCharsets.UTF_8);
			content.setText(rules);

			dismiss.setOnAction(e -> stage.close());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Parent root() {
		return root;
	}
}
