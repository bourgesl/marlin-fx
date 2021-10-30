package test;

import java.util.List;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.stage.Stage;

/**
 * A sample that demonstrates two path shapes.
 */
public class PathApp extends Application {

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(505, 300);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // Create path shape - square
        final Path path1 = new Path();
        path1.getElements().addAll(
                new MoveTo(25, 25),
                new HLineTo(65),
                new VLineTo(65),
                new LineTo(25, 65),
                new ClosePath());
        path1.setFill(null);
        path1.setStroke(Color.RED);
        path1.setStrokeWidth(2);

        // Create path shape - curves
        final Path path2 = new Path();
        path2.getElements().addAll(
                new MoveTo(100, 45),
                new CubicCurveTo(120, 20, 130, 80, 140, 45),
                new QuadCurveTo(150, 0, 160, 45),
                new ArcTo(20, 40, 0, 180, 45, true, true));
        path2.setFill(null);
        path2.setStroke(Color.DODGERBLUE);
        path2.setStrokeWidth(2);
        path2.setTranslateY(36);

        // Create path shape - curves
        final Path path3 = new Path();
        path3.getElements().addAll(
                new MoveTo(100, 45),
                new CubicCurveTo(120, 20, 130, 80, 140, 45),
                new QuadCurveTo(150, 0, 160, 45),
                new ArcTo(20, 40, 0, 180, 45, true, true));
        path3.setFill(Color.ORANGE);
        path3.setStroke(null);
        path3.setTranslateY(136);

        final Path path4 = new Path();
        path4.setFill(null);
        path4.setStroke(Color.GREEN);
        path4.setStrokeWidth(2);

        final List<PathElement> pe = path4.getElements();
        pe.add(new MoveTo(100, 100));

        for (int i = 0; i < 20000; i++) {
            pe.add(new LineTo(110 + 0.01f * i, 110));
            pe.add(new LineTo(111 + 0.01f * i, 100));
        }

        pe.add(new LineTo(Float.NaN, 200));
        pe.add(new LineTo(200, 200));
        pe.add(new LineTo(200, Float.NaN));
        pe.add(new LineTo(300, 300));
        pe.add(new LineTo(Float.NaN, Float.NaN));
        pe.add(new LineTo(100, 200));
        pe.add(new ClosePath());

// Test basic horizontal line (on pixel centers)
        final Path path5 = new Path();
        path5.getElements().addAll(
                new MoveTo(9.5, 9.5),
                new HLineTo(100));
        path5.setStroke(Color.PINK);
        path5.setStrokeWidth(1.0);

        // Fill contiguous shapes on NonAA rasterizer:
        double[] x = new double[] { 33.3333, 100.56677};

        final Path path6 = new Path();
        path6.getElements().addAll(
                new MoveTo(9.5, 9.5),
                new HLineTo(100),
                new LineTo(x[0], x[1]),
                new HLineTo(9.5),
                new ClosePath());
        path6.setFill(Color.RED);
        path6.setStroke(null);
        path6.setSmooth(false);

        final Path path7 = new Path();
        path7.getElements().addAll(
                new MoveTo(200, 9.5),
                new HLineTo(100),
                new LineTo(x[0], x[1]),
                new HLineTo(200),
                new ClosePath());
        path7.setFill(Color.BLUE);
        path7.setStroke(null);
        path7.setSmooth(false);

        root.getChildren().addAll(path6, path7, path1, path2, path3, path4, path5);
        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {

        System.setProperty("prism.verbose", "true");

        launch(args);
    }
}