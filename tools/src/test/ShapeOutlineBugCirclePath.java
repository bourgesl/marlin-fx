package test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.ClosePath;
import javafx.stage.Stage;

public class ShapeOutlineBugCirclePath extends Application {

    private final static double SIZE = 900;
    private final static double D = 0.5 * SIZE;
    private final static double sqrt2 = Math.sqrt(2);
    private static boolean SMALL = false;

    public static void main(String[] args) {
        SMALL = args.length > 0;
        Application.launch(args);
    }

    public static void printShape(Shape s) {
        if (!(s instanceof Path)) {
            System.out.println("Not a path: " + s);
            return;
        }
        System.out.println("Path[");
        for (PathElement pe : ((Path) s).getElements()) {
            if (pe instanceof MoveTo) {
                MoveTo mt = (MoveTo) pe;
                System.out.println("    MoveTo      (" + mt.getX() + ", " + mt.getY() + ")");
            } else if (pe instanceof LineTo) {
                LineTo lt = (LineTo) pe;
                System.out.println("    LineTo      (" + lt.getX() + ", " + lt.getY() + ")");
            } else if (pe instanceof QuadCurveTo) {
                QuadCurveTo lt = (QuadCurveTo) pe;
                System.out.println("    QuadCurveTo (" + lt.getControlX() + ", " + lt.getControlY() + ", ");
                System.out.println("                 " + lt.getX() + ", " + lt.getY() + ")");
            } else if (pe instanceof CubicCurveTo) {
                CubicCurveTo lt = (CubicCurveTo) pe;
                System.out.println("    CubicCurveTo(" + lt.getControlX1() + ", " + lt.getControlY1() + ", ");
                System.out.println("                 " + lt.getControlX2() + ", " + lt.getControlY2() + ", ");
                System.out.println("                 " + lt.getX() + ", " + lt.getY() + ")");
            } else if (pe instanceof ClosePath) {
                System.out.println("    ClosePath   ()");
            } else {
                System.out.println("Unrecognized path element: " + pe);
            }
        }
        System.out.println("]");
    }

    @Override
    public void start(Stage stage) throws Exception {
        double r = SMALL ? 100 : 1843200.0;
        double c = D - r / sqrt2;

        Circle circle = new Circle(c, c, r, Color.GREY);
        Circle littlecircle = new Circle(c, c, 10, Color.GREY);
        Shape shape = Shape.union(circle, littlecircle);
        printShape(shape);

        shape.setFill(Color.BLUE);
        shape.setStroke(Color.RED);
        shape.setStrokeWidth(2.0);
        shape.getStrokeDashArray().addAll(10.0, 5.0);

        Pane root = new Pane();
        root.getChildren().add(shape);

        stage.setScene(new Scene(root, SIZE, SIZE));
        stage.show();
    }
}
