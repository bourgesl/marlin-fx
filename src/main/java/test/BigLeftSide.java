package test;

import java.util.Arrays;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class BigLeftSide extends Application {
    public static final int NUM_SHAPES = 100;
    public static final int PATH_BORDER = 20;
    public static final int PATH_SIZE = 400;
    public static final int SHIFT_SIZE = 20;
    public static final long[] FRAME_TIMES = new long[60];
    public static final double NANO_SCALE = 1000.0 * 1000.0 * 1000.0;
    public static final double FRAME_AVG_SCALE = FRAME_TIMES.length * NANO_SCALE;

    @Override
    public void start(Stage stage) {
        int CV_DIM = PATH_BORDER + PATH_SIZE + SHIFT_SIZE + PATH_BORDER;
        Canvas cv = new Canvas(CV_DIM, CV_DIM);
        GraphicsContext gc = cv.getGraphicsContext2D();
        renderFrame(gc);

        Scene scene = new Scene(new Group(cv));
        stage.setScene(scene);
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            long nsStart;
            long nsAverageTotal;
            int delay = 20;

            @Override
            public void handle(long nsNow) {
                if (nsStart > 0) {
                    long nsFrame = (nsNow - nsStart);
                    if (delay > 0) {
                        delay--;
                        System.out.printf("Warming up with %f fps\n", NANO_SCALE / nsFrame);
                    } else {
                        if (nsAverageTotal == 0) {
                            Arrays.fill(FRAME_TIMES, nsFrame);
                            nsAverageTotal = nsFrame * FRAME_TIMES.length;
                        } else {
                            nsAverageTotal -= FRAME_TIMES[0];
                            System.arraycopy(FRAME_TIMES, 1, FRAME_TIMES, 0, FRAME_TIMES.length - 1);
                            FRAME_TIMES[FRAME_TIMES.length-1] = nsFrame;
                            nsAverageTotal += nsFrame;
                        }
                        System.out.printf("average frame rate = %f fps\n",
                                          (FRAME_AVG_SCALE / nsAverageTotal));
                    }
                }
                nsStart = nsNow;
                renderFrame(gc);
            }
        };
        timer.start();
    }

    void renderFrame(GraphicsContext gc) {
        int l1 = NUM_SHAPES / 4;
        int l2 = NUM_SHAPES / 2;
        int l3 = NUM_SHAPES * 3 / 4;
        int l4 = NUM_SHAPES;
        double s1 = l1;
        double s2 = l2 - l1;
        double s3 = l3 - l2;
        double s4 = l4 - l3;
        for (int i = 0; i < NUM_SHAPES; i++) {
            gc.save();
            gc.setFill(new Color(Math.random(), Math.random(), Math.random(), 1.0));
            double x, y;
            if (i < l1) {
                x = i / s1;
                y = 0.0;
            } else if (i < l2) {
                x = 1.0;
                y = (i - l1) / s2;
            } else if (i < l3) {
                x = (l3 - i) / s3;
                y = 1.0;
            } else {
                x = 0.0;
                y = (l4 - i) / s4;
            }
            gc.translate(PATH_BORDER + x * SHIFT_SIZE,
                         PATH_BORDER + y * SHIFT_SIZE);
            gc.beginPath();
            gc.moveTo(0.0, 0.0);
            gc.lineTo(1.0, 0.0);
            gc.lineTo(1.0, 1.0);
            gc.lineTo(0.0, 1.0);
            gc.closePath();
            gc.moveTo(PATH_SIZE, 0.0);
            gc.lineTo(PATH_SIZE, PATH_SIZE);
            gc.lineTo(PATH_SIZE - PATH_BORDER, PATH_SIZE);
            gc.lineTo(PATH_SIZE - PATH_BORDER, 0.0);
            gc.closePath();
            gc.fill();
            gc.restore();
        }
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {

        System.setProperty("prism.verbose", "true");

        launch(args);
    }
}
