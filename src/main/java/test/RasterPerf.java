package test;

import com.sun.javafx.geom.Ellipse2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.impl.shape.MarlinRasterizer;
import com.sun.prism.impl.shape.NativePiscesRasterizer;
import com.sun.prism.impl.shape.OpenPiscesRasterizer;
import com.sun.prism.impl.shape.ShapeRasterizer;

public class RasterPerf {

    static final int PRIME_CALLS = 5;
    static final long warmupns = 1000l * 1000l * 1000l;
    static final long targetns = 1000l * 1000l * 3000l;

    static String allresults = "";
    static final Path2D cubics, quads;

    static {
        cubics = new Path2D();
        cubics.moveTo(10, 10);
        cubics.curveTo(110, 10, 10, 15, 110, 20);
        cubics.curveTo(10, 20, 110, 25, 10, 30);
        cubics.curveTo(110, 30, 10, 35, 110, 40);
        cubics.curveTo(10, 40, 110, 45, 10, 50);
        cubics.curveTo(110, 50, 10, 55, 110, 60);
        cubics.curveTo(10, 60, 110, 65, 10, 70);
        cubics.curveTo(110, 70, 10, 75, 110, 80);
        cubics.curveTo(10, 80, 110, 85, 10, 90);
        cubics.curveTo(110, 90, 10, 95, 110, 100);
        cubics.curveTo(10, 100, 110, 105, 10, 110);
        quads = new Path2D();
        quads.moveTo(60, 10);
        quads.quadTo(110, 15, 60, 20);
        quads.quadTo(10, 25, 60, 30);
        quads.quadTo(110, 35, 60, 40);
        quads.quadTo(10, 45, 60, 50);
        quads.quadTo(110, 55, 60, 60);
        quads.quadTo(10, 65, 60, 70);
        quads.quadTo(110, 75, 60, 80);
        quads.quadTo(10, 85, 60, 90);
        quads.quadTo(110, 95, 60, 100);
        quads.quadTo(10, 105, 60, 110);
    }

    public static void bench(ShapeRasterizer sr, Shape s, boolean aa, String name) {
        RectBounds b = s.getBounds();
        for (int i = 0; i < PRIME_CALLS; i++) {
            sr.getMaskData(s, null, b, BaseTransform.IDENTITY_TRANSFORM, true, aa);
        }
        long start, elapsed;
        int n = 0;
        start = System.nanoTime();
        do {
            sr.getMaskData(s, null, b, BaseTransform.IDENTITY_TRANSFORM, true, aa);
            n++;
            elapsed = System.nanoTime() - start;
        } while (elapsed < warmupns);
        long limit = targetns * n / elapsed;
        System.out.println("warmup: " + n + " iterations in " + elapsed + "ns");
        System.out.println("benchmarking " + limit + " iterations");
        System.out.flush();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        start = System.nanoTime();
        for (int i = 0; i < limit; i++) {
            sr.getMaskData(s, null, b, BaseTransform.IDENTITY_TRANSFORM, true, aa);
        }
        long end = System.nanoTime();
        double ms = (end - start) / 1000.0 / 1000.0;
        ms = Math.round(ms * 100.0) / 100.0;
        String result = limit + " " + name + " rasterizations took "
                + ms + "ms, " + (limit * 1000) / ms + " ops/sec\n";
        System.out.println(result);
        allresults += result;
    }

    public static void bench(ShapeRasterizer sr, String srname, boolean aa) {
        bench(sr, new RoundRectangle2D(10, 10, 10, 10, 0, 0), aa, "10x10 Rectangle " + srname);
        bench(sr, new RoundRectangle2D(10, 10, 100, 100, 0, 0), aa, "100x100 Rectangle " + srname);
        bench(sr, new RoundRectangle2D(10, 10, 300, 300, 0, 0), aa, "300x300 Rectangle " + srname);
        bench(sr, new Ellipse2D(10, 10, 10, 10), aa, "10x10 Ellipse " + srname);
        bench(sr, new Ellipse2D(10, 10, 100, 100), aa, "100x100 Ellipse " + srname);
        bench(sr, new Ellipse2D(10, 10, 300, 300), aa, "300x300 Ellipse " + srname);
        bench(sr, cubics, aa, "100x100 Cubics " + srname);
        bench(sr, quads, aa, "100x100 Quads " + srname);
    }

    public static void bench(ShapeRasterizer sr, String srname) {
        bench(sr, srname + " non-AA", false);
        bench(sr, srname + " AA", true);
    }

    public static void main(String argv[]) {
        bench(new NativePiscesRasterizer(), "native");
        bench(new OpenPiscesRasterizer(), "Java");
        bench(new MarlinRasterizer(), "Marlin");
        System.out.println();
        System.out.println(allresults);
    }
}
