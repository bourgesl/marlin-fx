package test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

public class ShapeOutlineBugRectangle extends Application {
	private final static double SIZE = 900 * 1024 * 5;

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Path shape = new Path(new MoveTo(450, 450), new LineTo(-SIZE, -SIZE), new LineTo(0, -2 * SIZE), new LineTo(SIZE, -SIZE), new LineTo(450, 450), new ClosePath());

		shape.setFill(Color.BLUE);
		shape.setStroke(Color.RED);
		shape.setStrokeWidth(2.0);
		shape.getStrokeDashArray().addAll(10.0, 5.0);

		Pane root = new Pane();
		root.getChildren().add(shape);

		stage.setScene(new Scene(root, 900, 900));
		stage.show();
	}
}
