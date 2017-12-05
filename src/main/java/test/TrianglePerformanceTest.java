package test;

import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class TrianglePerformanceTest extends Application {

    private final static double WIDTH = 800;
    private final static double HEIGHT = 800;
    private final static double SIZE = 50;
    private final static double TRANSLATE_SIZE = 20;
    private final static double NOMINAL_SCALE = 0.5;
    private final static boolean CONTINUOUS_ROTATION = true;
    private final static boolean CONTINUOUS_SCALING = true;

    // null to disable stroke (inner)
    private final static Color STROKE_COLOR = null; // Color.BLACK;

    private final StackPane graphicsPane = new StackPane();
    private final Pane drawingPane = new Pane();
    private final Group graphics = new Group();

    private ToolBar toolBar;
    private TextField numElemSelection;
    private TextField frameRate;
    private ToggleButton triRectButton;
    private ToggleButton colorButton;
    private ToggleButton animButton;
    private ToggleButton oneShapeButton;
    private ToggleButton spinButton;

    private static int numElements = 1000;
    private static boolean showRectangles = false;
    private static boolean randomColors = true;
    private static boolean oneShape = false;
    private static boolean doSpin = true;

    // Fixed seed to avoid any difference between runs:
    private final Random random = new Random(13L);
    private final Random random2 = new Random(131L);

    private class MyAnimationTimer extends AnimationTimer {
        private final static double SECONDS_TO_NANOS = 1e9;
        private final static long INSTANT_TO_NANOS = 500_000_000L;

        private boolean active = false;
/*
        private long previousTimeNanos = 0;
        private long averageDeltaNanos = 0;
*/
        private int nbFrames = 0;

        private long lastInstant = System.nanoTime();
        private long nextInstant = lastInstant + INSTANT_TO_NANOS;


        @Override
        public void start() {
            super.start();
            this.active = true;
        }

        @Override
        public void stop() {
            super.stop();
            this.active = false;
        }

        @Override
        public void handle(long nowNanos) {
/*
            if (previousTimeNanos > 0) {
                long deltaNanos = nowNanos - previousTimeNanos;
                if (deltaNanos > 0) {
                    if (AVERAGE_FRAME_RATE) {
                        averageDeltaNanos = (averageDeltaNanos > 0) ? (59 * averageDeltaNanos + deltaNanos) / 60 : deltaNanos;
                    } else {
                        averageDeltaNanos = deltaNanos;
                    }
                    frameRate.setText(String.format("%.2f", SECONDS_TO_NANOS / averageDeltaNanos));
                }
            }
            previousTimeNanos = nowNanos;
*/
            nbFrames++;

            if (nowNanos > nextInstant) {
                final String fps = String.format("%.2f", (SECONDS_TO_NANOS * nbFrames) / (nowNanos - lastInstant));
                frameRate.setText(fps);

                System.out.println(fps);

                // reset
                nbFrames = 0;
                lastInstant = nowNanos;
                nextInstant = nowNanos + INSTANT_TO_NANOS;
            }

            double seconds = nowNanos / SECONDS_TO_NANOS;
            if (doSpin) {
                double angle = 10 * seconds;
                double scale = NOMINAL_SCALE + 0.1*Math.sin(seconds);
                if (CONTINUOUS_ROTATION) {
                    setRotation(angle);
                }
                if (CONTINUOUS_SCALING) {
                    setScale(scale);
                }
                setTranslation(0.0, 0.0);
            } else {
                double xt, yt, phase;
                phase = seconds % 4.0;
                if (phase < 0.0) {
                    phase += 4.0;
                }
                if (phase < 1.0) {
                    xt = phase;
                    yt = 0.0;
                } else if (phase < 2.0) {
                    xt = 1.0;
                    yt = phase - 1.0;
                } else if (phase < 3.0) {
                    xt = 3.0 - phase;
                    yt = 1.0;
                } else {
                    xt = 0.0;
                    yt = 4.0 - phase;
                }
                setRotation(0.0);
                setScale(NOMINAL_SCALE);
                setTranslation((xt - 0.5) * TRANSLATE_SIZE, (yt - 0.5) * TRANSLATE_SIZE);
            }
        }
        public boolean isActive() {
            return active;
        }
    };

    MyAnimationTimer timer = new MyAnimationTimer();

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle(getClass().getSimpleName());

        BorderPane root = new BorderPane();

        drawingPane.getChildren().add(graphics);

        graphicsPane.getChildren().add(drawingPane);

        final Font font = new Font(20);

        toolBar = new ToolBar();
        root.setTop(toolBar);
        root.setCenter(graphicsPane);

        {
            // Number of elements:
            Label label = new Label("Num. Elem.:");
            label.setFont(font);
            numElemSelection = new TextField();
            numElemSelection.setFont(font);
            numElemSelection.setAlignment(Pos.BASELINE_RIGHT);
            numElemSelection.setPrefColumnCount(4);
            numElemSelection.setText(String.valueOf(numElements));
            numElemSelection.setOnAction(resetConfigActionHandler);
            toolBar.getItems().addAll(label, numElemSelection);
        }

        {
            // Frame rate:
            Label label = new Label("Frame rate:");
            label.setFont(font);
            frameRate = new TextField();
            frameRate.setFont(font);
            frameRate.setAlignment(Pos.BASELINE_RIGHT);
            frameRate.setPrefColumnCount(4);
            frameRate.setEditable(false);
            toolBar.getItems().addAll(label, frameRate);
        }

        toolBar.getItems().add(new Label(" "));

        {
            // Triangles/Rectangles toggle button:
            triRectButton = new ToggleButton("Triangles/Rectangles");
            triRectButton.setFont(font);
            triRectButton.setSelected(showRectangles);
            triRectButton.setOnAction(resetConfigActionHandler);
            toolBar.getItems().add(triRectButton);
        }

        {
            // Random color button
            colorButton = new ToggleButton("Random Color");
            colorButton.setFont(font);
            colorButton.setSelected(randomColors);
            colorButton.setOnAction(resetConfigActionHandler);
            toolBar.getItems().add(colorButton);
        }

        {
            // Spin vs translate button
            spinButton = new ToggleButton("Spin");
            spinButton.setFont(font);
            spinButton.setSelected(doSpin);
            spinButton.setOnAction(animationActionHandler);
            toolBar.getItems().add(spinButton);
        }

        {
            // Single shape button
            oneShapeButton = new ToggleButton("One Shape");
            oneShapeButton.setFont(font);
            oneShapeButton.setSelected(oneShape);
            oneShapeButton.setOnAction(resetConfigActionHandler);
            toolBar.getItems().add(oneShapeButton);
        }

        {
            // Start/Stop animation toggle button:
            animButton = new ToggleButton("Start/Stop Animation");
            animButton.setFont(font);
            animButton.setSelected(true);
            animButton.setOnAction(animationActionHandler);
            toolBar.getItems().add(animButton);
        }

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.show();

        drawingPane.widthProperty().addListener((v,o,n) -> resetConfig());
        drawingPane.heightProperty().addListener((v,o,n) -> resetConfig());

        resetConfig();
    }

    private void setTranslation(double x, double y) {
        graphics.setTranslateX(x);
        graphics.setTranslateY(y);
    }

    private void setScale(double value) {
        graphics.setScaleX(value);
        graphics.setScaleY(value);
    };

    private void setRotation(double angle) {
        graphics.setRotate(angle);
    }

    private EventHandler<ActionEvent> resetConfigActionHandler = e -> resetConfig();

    private EventHandler<ActionEvent> animationActionHandler = e -> {
        doSpin = spinButton.isSelected();
        boolean showAnimation = animButton.isSelected();
        if (showAnimation != timer.isActive()) {
            if (showAnimation) {
                timer.start();
            } else {
                timer.stop();
            }
        }
    };

    private void resetConfig() {
        try {
            timer.stop();
            numElements = Math.abs(Integer.valueOf(numElemSelection.getText()));
            showRectangles = triRectButton.isSelected();
            randomColors = colorButton.isSelected();
            oneShape = oneShapeButton.isSelected();
            boolean showAnimation = animButton.isSelected();
            updateGraphics();
            if (showAnimation) {
                timer.start();
            } else {
                timer.stop();
            }
        } catch (NumberFormatException e) {
            System.err.println("Illegal value for number of elements: " + numElemSelection.getText());
        }
    }

    private void updateGraphics() {
        graphics.getChildren().clear();
        setRotation(0.0);
        setScale(NOMINAL_SCALE);

        Path p;
        if (oneShape) {
            p = new Path();
            setShapeAttrs(p);
            graphics.getChildren().add(p);
        } else {
            p = null;
        }

        for (int i = 0; i < numElements; i++) {
            addShape(graphics, drawingPane.getWidth(), drawingPane.getHeight(), p, showRectangles, randomColors);
        }
    }

    private void setShapeAttrs(Shape shape) {
        shape.setFill(randomColors ? randomColor() : Color.BLUE);
        if (STROKE_COLOR != null) {
            shape.setStroke(STROKE_COLOR);
            shape.setStrokeType(StrokeType.INSIDE);
            shape.setStrokeWidth(0.5);
        }
    }

    private void addShape(Group graphics, double width, double height,
                          Path p,
                          boolean showRectangles, boolean randomColors)
    {
        final double px0 = random() * width;
        final double py0 = random() * height;
        final double px1 = px0 - SIZE/2 + random() * SIZE;
        final double py1 = py0 - SIZE/2 + random() * SIZE;
        final double px2 = px0 - SIZE/2 + random() * SIZE;
        final double py2 = py0 - SIZE/2 + random() * SIZE;

        Shape shape = null;

        if (!showRectangles) {
            if (p == null) {
                shape = new Polygon(px0, py0, px1, py1, px2, py2);
            } else {
                p.getElements().add(new MoveTo(px0, py0));
                p.getElements().add(new LineTo(px1, py1));
                p.getElements().add(new LineTo(px2, py2));
            }
        } else {
            double boundsX = Math.min(Math.min(px0, px1), px2);
            double boundsY = Math.min(Math.min(py0, py1), py2);
            double boundsWidth = Math.max(Math.max(px0, px1), px2) - boundsX;
            double boundsHeight = Math.max(Math.max(py0, py1), py2) - boundsY;
            if (p == null) {
                shape = new Rectangle(boundsX, boundsY, boundsWidth, boundsHeight);
            } else {
                p.getElements().add(new MoveTo(boundsX, boundsY));
                p.getElements().add(new HLineTo(boundsX + boundsWidth));
                p.getElements().add(new VLineTo(boundsY + boundsHeight));
                p.getElements().add(new HLineTo(boundsX));
            }
        }

        if (shape != null) {
            setShapeAttrs(shape);
            graphics.getChildren().add(shape);
        }
    }

    private double random() {
        return random.nextDouble();
    }

    private Color randomColor() {
        return new Color(random2.nextDouble(), random2.nextDouble(), random2.nextDouble(), 1.0);
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("-oneshape")) {
                oneShape = true;
            } else if (arg.equalsIgnoreCase("-manyshapes")) {
                oneShape = false;
            } else if (arg.equalsIgnoreCase("-triangles")) {
                showRectangles = false;
            } else if (arg.equalsIgnoreCase("-rects")) {
                showRectangles = true;
            } else if (arg.equalsIgnoreCase("-randomcolor")) {
                randomColors = true;
            } else if (arg.equalsIgnoreCase("-singlecolor")) {
                randomColors = false;
            } else if (arg.equalsIgnoreCase("-spin")) {
                doSpin = true;
            } else if (arg.equalsIgnoreCase("-nospin")) {
                doSpin = false;
            } else if (arg.equalsIgnoreCase("-numshapes")) {
                if (++i >= args.length) {
                    throw new RuntimeException("-numshapes argument requires numeric parameter");
                } else {
                    numElements = Integer.parseInt(args[i]);
                }
            }
        }
        launch(args);
    }

}

