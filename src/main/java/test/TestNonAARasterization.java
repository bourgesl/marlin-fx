package test;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.Random;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class TestNonAARasterization extends Application {

    static enum ShapeMode {
        TWO_CUBICS,
        FOUR_QUADS,
        FIVE_LINE_POLYS,
        NINE_LINE_POLYS,
        RECTANGLES,
        OVALS,
        OCTAGONS,
    }
    static final double OCT_C = 1.0 / (2.0 + Math.sqrt(2.0));

    // original thresholds:
    static double tolerance = 0.0001;
    static double warn      = 0.0005;

    static ShapeMode shapemode = ShapeMode.OVALS; // TWO_CUBICS;
    static boolean useJava2D = false;
    static boolean useJava2DClip = false;
    static boolean exitWhenDone = false;

    static final int NUMTESTS = 100000;
    static final int TESTW = 50;
    static final int TESTH = 50;
    static final int MAG = 18;

    static final int BGPIXEL = 0xffffffff;
    static final int FGPIXEL = 0xff000000;

    static final Color BAD = Color.RED;
    static final Color GOOD = Color.GREEN;
    static final Color OK = Color.ORANGE;
    static final Color IN = Color.YELLOW;
    static final Color OUT = Color.WHITE;
    static final Color GRAY = Color.CYAN;

    static final long SEED = 1666133789L;
    static final Random RAND;

    static {
        RAND = new Random(SEED);
    }

    public static final class Result {

        final Path2D path2d;
        final int numerrors;
        final int numwarnings;

        public Result(Path2D path2d, int numerrors, int numwarnings) {
            this.path2d = path2d;
            this.numerrors = numerrors;
            this.numwarnings = numwarnings;
        }

        public boolean worseThan(Result other) {
            if (other == null) {
                return true;
            }
            if (numerrors > other.numerrors) {
                return true;
            }
            if (numerrors < other.numerrors) {
                return false;
            }
            return numwarnings > other.numwarnings;
        }
    }

    int numBadPaths = 0;
    Result worst;
    int numpathstested;
    long totalerrors;
    long totalwarnings;
    Canvas resultcv;
    Path resultpath;
    Text resulttext;
    Text progressLabel;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        progressLabel = new Text("running...");
        root.setBottom(progressLabel);

        resultcv = new Canvas(TESTW * MAG, TESTH * MAG);
        resultpath = new Path();
        resultpath.setCache(false);
        resultpath.setSmooth(false);
        resultpath.getTransforms().add(new Scale(MAG, MAG));
        resultpath.setFill(null);
        resultpath.setStroke(Color.DARKGRAY);
        resultpath.setStrokeWidth(1.0 / MAG);
        root.setCenter(new Group(resultcv, resultpath));

        HBox legend = new HBox();
        legend.getChildren().add(new Text("Good Pixel: "));
        legend.getChildren().add(makeStatusCanvas(FGPIXEL, true, false));
        legend.getChildren().add(new Text("   Bad Pixels: "));
        legend.getChildren().add(makeStatusCanvas(FGPIXEL, false, false));
        legend.getChildren().add(makeStatusCanvas(BGPIXEL, true, false));
        legend.getChildren().add(new Text("   Iffy Pixels: "));
        legend.getChildren().add(makeStatusCanvas(FGPIXEL, false, true));
        legend.getChildren().add(makeStatusCanvas(BGPIXEL, true, true));
        legend.getChildren().add(new Text("   Alpha(AA) Pixel: "));
        legend.getChildren().add(makeStatusCanvas(12, true, false));
        resulttext = new Text("(waiting for result)");
        VBox vb = new VBox(legend, resulttext);
        vb.setAlignment(Pos.TOP_CENTER);
        vb.setFillWidth(false);
        root.setTop(vb);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Testing " + shapemode);
        stage.show();

        resultcv.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dumpTest();
            }
        });

        Thread t = new Thread(() -> generatePaths());
        t.setDaemon(true);
        t.start();
    }

    static final String svgcommand = "MLQCZ";
    static final int[] coordcount = {2, 2, 4, 6, 0};
    static final String[] pathCommands = new String[] {"moveTo", "lineTo", "quadTo", "curveTo", "closePath"};

    public void dumpTest() {
        dumpResult(worst);
    }

    static void dumpResult(Result r) {
        if (r != null) {
            Path2D p2d = r.path2d;
            double[] coords = new double[6];
            System.out.println("test case with "
                    + r.numerrors + " errors and "
                    + r.numwarnings + " warnings:");
            System.out.print("Path=");
            PathIterator pi = p2d.getPathIterator(null);
            while (!pi.isDone()) {
                int type = pi.currentSegment(coords);
                System.out.print(' ');
                System.out.print(svgcommand.charAt(type));
                for (int i = 0; i < coordcount[type]; i++) {
                    if (i > 0) {
                        System.out.print(',');
                    }
                    System.out.print(coords[i]);
                }
                pi.next();
            }
            System.out.println();
            // Path2D:
            System.out.println("Path2D p = new Path2D.Double();");
            pi = p2d.getPathIterator(null);
            while (!pi.isDone()) {
                int type = pi.currentSegment(coords);
                System.out.print("p.");
                System.out.print(pathCommands[type]);
                System.out.print("(");
                for (int i = 0; i < coordcount[type]; i++) {
                    if (i > 0) {
                        System.out.print(", ");
                    }
                    System.out.print(coords[i]);
                }
                System.out.println(");");
                pi.next();
            }
        }
    }

    public void clear() {
        GraphicsContext gc = resultcv.getGraphicsContext2D();
        gc.clearRect(0, 0, resultcv.getWidth(), resultcv.getHeight());
    }

    private BufferedImage bimgT = null;
    private Graphics2D g2dT = null;

    private BufferedImage bimgD = null;
    private Graphics2D g2dD = null;

    public void renderPath(Path2D p2d, Path p, WritableImage wimg, boolean test) {
        if (useJava2D) {
            final BufferedImage bimg;
            final Graphics2D g2d;
            if (test) {
                if (bimgT == null) {
                    bimgT = new BufferedImage(TESTW, TESTH, BufferedImage.TYPE_INT_ARGB);
                    g2dT = bimgT.createGraphics();
                    if (true) {
                        g2dT.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                    g2dT.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
                    g2dT.setBackground(java.awt.Color.WHITE);
                    g2dT.setColor(java.awt.Color.BLACK);
                }
                bimg = bimgT;
                g2d = g2dT;
            } else {
                // use another image:
                if (bimgD == null) {
                    bimgD = new BufferedImage(TESTW, TESTH, BufferedImage.TYPE_INT_ARGB);
                    g2dD = bimgD.createGraphics();
                    g2dD.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
                    g2dD.setBackground(java.awt.Color.WHITE);
                    g2dD.setColor(java.awt.Color.BLACK);
                }
                bimg = bimgD;
                g2d = g2dD;
            }
            g2d.clearRect(0, 0, TESTW, TESTH);
            if (useJava2DClip) {
                g2d.setClip(p2d);
                g2d.fillRect(0, 0, TESTW, TESTH);
                g2d.setClip(null);
            } else {
                g2d.fill(p2d);
            }
            copy(bimg, wimg);
        } else {
            setPath(p, p2d);
            SnapshotParameters sp = new SnapshotParameters();
            sp.setViewport(new Rectangle2D(0, 0, TESTW, TESTH));
            p.snapshot(sp, wimg);
        }
    }

    public static boolean near(Path2D p, double x, double y, double err) {
        if (err == 0.0) {
            return false;
        }
        return p.intersects(x - err, y - err, err * 2.0, err * 2.0)
                && !p.contains(x - err, y - err, err * 2.0, err * 2.0);
    }

    public void update(Path2D p2d) {
        setPath(resultpath, p2d);
        Path p = makePath();
        WritableImage wimg = new WritableImage(TESTW, TESTH);
        renderPath(p2d, p, wimg, false);
        PixelReader pr = wimg.getPixelReader();
        GraphicsContext gc = resultcv.getGraphicsContext2D();
        gc.save();
        for (int y = 0; y < TESTH; y++) {
            for (int x = 0; x < TESTW; x++) {
                boolean inpath = p2d.contains(x + 0.5, y + 0.5);
                boolean nearpath = near(p2d, x + 0.5, y + 0.5, warn);
                int pixel = pr.getArgb(x, y);
                renderPixelStatus(gc, x, y, pixel, inpath, nearpath);
            }
        }
        gc.restore();
    }

    Canvas makeStatusCanvas(int pixel, boolean inpath, boolean nearpath) {
        Canvas cv = new Canvas(MAG, MAG);
        renderPixelStatus(cv.getGraphicsContext2D(), 0, 0, pixel, inpath, nearpath);
        return cv;
    }

    void renderPixelStatus(GraphicsContext gc, int x, int y, int pixel, boolean inpath, boolean nearpath) {
        Color cross = null;
        Color circle = null;
        Color fill;
        if (pixel == FGPIXEL) {
            fill = IN;
            if (inpath) {
                cross = GOOD;
            } else if (nearpath) {
                circle = OK;
            } else {
                cross = BAD;
            }
        } else if (pixel == BGPIXEL) {
            fill = OUT;
            if (inpath) {
                if (nearpath) {
                    circle = OK;
                } else {
                    cross = BAD;
                }
            }
        } else {
            fill = GRAY;
            if (nearpath) {
                circle = OK;
            } else {
                cross = BAD;
            }
        }
        gc.setFill(fill);
        gc.fillRect(x * MAG, y * MAG, MAG, MAG);
        if (cross != null) {
            gc.setFill(cross);
            gc.fillRect(x * MAG + 2, y * MAG + MAG / 2, MAG - 4, 1);
            gc.fillRect(x * MAG + MAG / 2, y * MAG + 2, 1, MAG - 4);
        }
        if (circle != null) {
            gc.setStroke(circle);
            gc.strokeOval(x * MAG + 2, y * MAG + 2, MAG - 4, MAG - 4);
        }
    }

    public void updateLabel() {
        int numbadpaths = numBadPaths;
        double percentbadpaths = numbadpaths * 100.0 / numpathstested;
        double avgbadpixels = (totalerrors == 0) ? 0.0 : totalerrors * 1.0 / numbadpaths;
        double avgwarnings = (totalwarnings == 0) ? 0.0 : totalwarnings * 1.0 / numbadpaths;
        String progress = String.format("bad paths (%d/%d == %3.2f%%",
                numbadpaths, numpathstested, percentbadpaths);
        progress += String.format(", %d bad pixels (avg = %3.2f - max =  %d)", totalerrors, avgbadpixels,
                (worst != null) ? worst.numerrors : 0);
        progress += String.format(", %d warnings (avg = %3.2f)", totalwarnings, avgwarnings);
        progressLabel.setText(progress);
    }

    public void update() {
        clear();
        if (worst != null) {
            update(worst.path2d);
            resulttext.setText(worst.numerrors + " bad pixels, " + worst.numwarnings + " iffy pixels");
        }
    }

    public static void setPath(Path p, Path2D p2d) {
        p.getElements().clear();
        PathIterator pi = p2d.getPathIterator(null);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    p.getElements().add(new MoveTo(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    p.getElements().add(new LineTo(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    p.getElements().add(new QuadCurveTo(coords[0], coords[1],
                            coords[2], coords[3]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    p.getElements().add(new CubicCurveTo(coords[0], coords[1],
                            coords[2], coords[3],
                            coords[4], coords[5]));
                    break;
                case PathIterator.SEG_CLOSE:
                    p.getElements().add(new ClosePath());
                    break;
                default:
                    throw new InternalError("unexpected segment type");
            }
            pi.next();
        }
        p.getElements().add(new ClosePath());
    }

    public Path makePath() {
        Path p = new Path();
        p.setFill(Color.BLACK);
        p.setStroke(null);
        p.setCache(false);
        p.setSmooth(false);
        return p;
    }

    public void addResult(Result r, int numtested) {
        numBadPaths++;
        if (r.worseThan(worst)) {
            worst = r;
//            dumpTest();
        }
        totalerrors += r.numerrors;
        totalwarnings += r.numwarnings;
        numpathstested = numtested;
        update();
        updateLabel();
    }

    public void updateProgress(int numtested) {
        numpathstested = numtested;
        updateLabel();
    }

    public static double rand(double d) {
        return RAND.nextDouble() * d;
    }

    boolean done;

    public synchronized void signalDone() {
        done = true;
        notifyAll();
    }

    public synchronized void waitDone() throws InterruptedException {
        while (!done) {
            wait();
        }
    }

    public static void copy(BufferedImage bimg, WritableImage wimg) {
        PixelWriter pw = wimg.getPixelWriter();
        for (int y = 0; y < TESTH; y++) {
            for (int x = 0; x < TESTW; x++) {
                pw.setArgb(x, y, bimg.getRGB(x, y));
            }
        }
    }

    static final Ellipse2D e2d = new Ellipse2D.Double();

    static void genShape(Path2D p2d, ShapeMode mode) {
        double rx, ry, rw, rh;
        p2d.reset();
        switch (mode) {
            case TWO_CUBICS:
                p2d.moveTo(rand(TESTW), rand(TESTH));
                p2d.curveTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                p2d.curveTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                break;
            case FOUR_QUADS:
                p2d.moveTo(rand(TESTW), rand(TESTH));
                p2d.quadTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                p2d.quadTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                p2d.quadTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                p2d.quadTo(rand(TESTW), rand(TESTH), rand(TESTW), rand(TESTH));
                break;
            case NINE_LINE_POLYS:
            case FIVE_LINE_POLYS:
                p2d.moveTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                if (shapemode == ShapeMode.FIVE_LINE_POLYS) {
                    // And an implicit close makes 5 lines
                    break;
                }
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                p2d.lineTo(rand(TESTW), rand(TESTH));
                // And an implicit close makes 9 lines
                break;
            case RECTANGLES:
                rw = rand(TESTW);
                rh = rand(TESTH);
                rx = rand(TESTW - rw);
                ry = rand(TESTH - rh);
                p2d.moveTo(rx, ry);
                p2d.lineTo(rx + rw, ry);
                p2d.lineTo(rx + rw, ry + rh);
                p2d.lineTo(rx, ry + rh);
                break;
            case OVALS:
                rw = rand(TESTW);
                rh = rand(TESTH);
                rx = rand(TESTW - rw);
                ry = rand(TESTH - rh);
                e2d.setFrame(rx, ry, rw, rh);
                p2d.append(e2d, false);
                break;
            case OCTAGONS:
                rw = rand(TESTW);
                rh = rand(TESTH);
                rx = rand(TESTW - rw);
                ry = rand(TESTH - rh);
                double ow = rw * OCT_C;
                double oh = rh * OCT_C;
                p2d.moveTo(rx + ow, ry);
                p2d.lineTo(rx + rw - ow, ry);
                p2d.lineTo(rx + rw, ry + oh);
                p2d.lineTo(rx + rw, ry + rh - oh);
                p2d.lineTo(rx + rw - ow, ry + rh);
                p2d.lineTo(rx + ow, ry + rh);
                p2d.lineTo(rx, ry + rh - oh);
                p2d.lineTo(rx, ry + oh);
                break;
        }
    }

    public void generatePaths() {
        final Path2D p2d = new Path2D.Double();
        final Path p = makePath();
        final WritableImage wimg = new WritableImage(TESTW, TESTH);
        final PixelReader pr = wimg.getPixelReader();
        int n = 0;
        while (n < NUMTESTS) {
            genShape(p2d, shapemode);
            done = false;
            Platform.runLater(() -> {
                renderPath(p2d, p, wimg, true);
                signalDone();
            });
            try {
                waitDone();
            } catch (InterruptedException ex) {
                break;
            }
            int errors = 0;
            int warnings = 0;
            for (int y = 0; y < TESTH; y++) {
                for (int x = 0; x < TESTW; x++) {
                    boolean inpath = p2d.contains(x + 0.5, y + 0.5);
                    int pixel = pr.getArgb(x, y);
                    if (pixel == FGPIXEL) {
                        if (!inpath) {
                            if (near(p2d, x + 0.5, y + 0.5, warn)) {
                                if (!near(p2d, x + 0.5, y + 0.5, tolerance)) {
                                    warnings++;
                                }
                            } else {
                                errors++;
                            }
                        }
                    } else if (pixel == BGPIXEL) {
                        if (inpath) {
                            if (near(p2d, x + 0.5, y + 0.5, warn)) {
                                if (!near(p2d, x + 0.5, y + 0.5, tolerance)) {
                                    warnings++;
                                }
                            } else {
                                errors++;
                            }
                        }
                    } else {
                        errors++;
                    }
                }
            }
            ++n;
            if (warnings + errors > 0) {
                final int numtested = n;
                final Result r = new Result(new Path2D.Double(p2d), errors, warnings);
                Platform.runLater(() -> {
                    addResult(r, numtested);
                });
            } else if (n % 100 == 0) {
                final int numtested = n;
                Platform.runLater(() -> updateProgress(numtested));
            }
        }
        Platform.runLater(() -> {
            System.out.println(progressLabel.getText());
            dumpTest();
        });
        if (exitWhenDone) {
            Platform.exit();
        }
    }

    static void usage(int code) {
        System.out.println("java TestNonAARasterization [-tolerance <double>] [-warn <double>]");
        System.out.println("     [-rect|-oval|-octagon|-quad|-cubic|-poly|-bigpoly]");
        System.out.println("     [-j2d|-j2dclip] [-exit]");
        System.exit(code);
    }

    public static void main(String argv[]) {
        if (argv.length > 0) {
            for (int i = 0; i < argv.length; i++) {
                String arg = argv[i];
                if (arg.equalsIgnoreCase("-tolerance")) {
                    if (++i >= argv.length) {
                        usage(-1);
                    }
                    tolerance = Double.parseDouble(argv[i]);
                } else if (arg.equalsIgnoreCase("-warn")) {
                    if (++i >= argv.length) {
                        usage(-1);
                    }
                    warn = Double.parseDouble(argv[i]);
                } else if (arg.equalsIgnoreCase("-poly")) {
                    shapemode = ShapeMode.FIVE_LINE_POLYS;
                } else if (arg.equalsIgnoreCase("-bigpoly")) {
                    shapemode = ShapeMode.NINE_LINE_POLYS;
                } else if (arg.equalsIgnoreCase("-rect")) {
                    shapemode = ShapeMode.RECTANGLES;
                } else if (arg.equalsIgnoreCase("-oval")) {
                    shapemode = ShapeMode.OVALS;
                } else if (arg.equalsIgnoreCase("-octagon")) {
                    shapemode = ShapeMode.OCTAGONS;
                } else if (arg.equalsIgnoreCase("-cubic")) {
                    shapemode = ShapeMode.TWO_CUBICS;
                } else if (arg.equalsIgnoreCase("-quad")) {
                    shapemode = ShapeMode.FOUR_QUADS;
                } else if (arg.equalsIgnoreCase("-j2d")) {
                    useJava2D = true;
                } else if (arg.equalsIgnoreCase("-j2dclip")) {
                    useJava2D = true;
                    useJava2DClip = true;
                } else if (arg.equalsIgnoreCase("-exit")) {
                    exitWhenDone = true;
                } else if (arg.equalsIgnoreCase("-help")) {
                    usage(0);
                } else {
                    usage(-1);
                }
            }
        }

        if (false) {
            // 2018 - lower thresholds:
            final Double dec = Double.parseDouble(System.getProperty("sun.java2d.renderer.cubic_dec_d2", "1.0"));
            System.out.println("dec bind: "+dec);

            warn = dec / 8; // e = 8 x dec_binD
            tolerance = warn * 0.9;
        }

        System.out.println("useJava2D: " + useJava2D);
        System.out.println("shapemode: " + shapemode);
        System.out.println("tolerance: " + tolerance);
        System.out.println("warn val : " + warn);

        launch(argv);
    }
}
