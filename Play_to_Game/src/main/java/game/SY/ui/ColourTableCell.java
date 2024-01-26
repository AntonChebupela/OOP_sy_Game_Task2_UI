package game.SY.ui;

import game.SY.model.Colour;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColourTableCell<S> extends TableCell<S, Colour> {

	@Override
	protected void updateItem(Colour item, boolean empty) {
		if (!empty) {
			Rectangle rectangle = new Rectangle(40, 20);
			rectangle.setFill(Color.valueOf(item.name()));
			rectangle.setStroke(Color.LIGHTGRAY);
			rectangle.setStrokeWidth(1);
			setGraphic(rectangle);
		}
		super.updateItem(item, empty);
	}
}
