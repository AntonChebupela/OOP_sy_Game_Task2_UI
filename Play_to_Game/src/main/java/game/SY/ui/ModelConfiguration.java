package game.SY.ui;

import java.time.Duration;

import game.SY.model.Transport;
import game.SY.ui.model.PlayerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import game.GameKT.graph.Graph;

public interface ModelConfiguration {

	ObjectProperty<Duration> timeoutProperty();

	ObservableList<Boolean> revealRounds();

	ObjectProperty<Graph<Integer, Transport>> graphProperty();

	ObservableList<PlayerProperty> allPlayers();

	ObservableList<PlayerProperty> players();
}
