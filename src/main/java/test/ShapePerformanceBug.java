package test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class ShapePerformanceBug extends Application {
	private final static double SIZE = 900;
	private final static double D = 0.5 * SIZE;
	private final static double sqrt2 = Math.sqrt(2);

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		double r = 1843200.0;
		double c = D - r/sqrt2;

		Circle shape = new Circle(c, c, r, Color.GREY);

		shape.setStroke(Color.BLACK);
		shape.setStrokeWidth(2.0);
		shape.getStrokeDashArray().addAll(10.0, 5.0);

		Pane root = new Pane();
		root.getChildren().add(shape);

		stage.setScene(new Scene(root, SIZE, SIZE));
		stage.show();
	}
}
