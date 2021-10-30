/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.stage.Stage;

/**
 *
 */
public class PolylineWideClipTest extends Application {

    private static final boolean TEST_FILL = true;
    private static final boolean TEST_STROKE = true; // stroker passes (2021.10.17)
    
    private static final boolean CLOSE = false;

    private static final int SPEED = 10; // 60 means 1s

    // (2^31 = 1073741824) / 256 = 4194304 => overflow in DRenderer
    // private static final double LARGE_X_COORDINATE = 4194304.250 + 1000.0013;
    // max precision limited by CurveClipSplitter: 0.9999999999999997 ~ 1E-15 EPS
    // => coords > ~1E15 will cause troubles (solver will give incorrect intersections 
    //   that could be fixed using recursive subdivision or newton root refinement)
    private static final double LARGE_X_COORDINATE = 1E15; // not ideal = Float.MAX_VALUE / 2.0
//    private static final double LARGE_X_COORDINATE = 1E12;

    private static final double SCENE_WIDTH = 1000.0;

    private static final int[][] combPts = new int[3][];
    private static final List<int[]> comb2PtsIn8;

    private static final double[][] ptsIn = new double[4][2];
    private static final double[][] ptsOut = new double[8][2];

    static final long SEED = 1666133789L;
    // Fixed seed to avoid any difference between runs:
    static final Random RANDOM = new Random(SEED);
    
    static {
        // 0 1 2 or 1 2 0 or 2 0 1
        combPts[0] = new int[]{0, 1, 2};
        combPts[1] = new int[]{1, 2, 0};
        combPts[2] = new int[]{2, 0, 1};
        // System.out.println("comb2PtsIn8: " + Arrays.deepToString(combPts));

        // Generate 1 pt inside:
        ptsIn[0][0] = SCENE_WIDTH * 1 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();
        ptsIn[0][1] = SCENE_WIDTH * 1 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();

        ptsIn[1][0] = SCENE_WIDTH * 2 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();
        ptsIn[1][1] = SCENE_WIDTH * 1 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();

        ptsIn[2][0] = SCENE_WIDTH * 2 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();
        ptsIn[2][1] = SCENE_WIDTH * 2 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();

        ptsIn[3][0] = SCENE_WIDTH * 1 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();
        ptsIn[3][1] = SCENE_WIDTH * 2 / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble();

        System.out.println("ptsIn: " + Arrays.deepToString(ptsIn));

        // Generate 1 pt outside in each quadrant outside:
        comb2PtsIn8 = generateCombinations(8, 2);
        /*
        System.out.println("comb2PtsIn8: " + comb2PtsIn8.size());
        for (int[] comb2Pts : comb2PtsIn8) {
            System.out.println(Arrays.toString(comb2Pts));
        }
         */

        int i = 0;
        // LEFT
        ptsOut[i][0] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = (SCENE_WIDTH / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble());
        i++;

        // TOP LEFT
        ptsOut[i][0] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        // TOP
        ptsOut[i][0] = (SCENE_WIDTH / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble());
        ptsOut[i][1] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        // TOP RIGHT
        ptsOut[i][0] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        // RIGHT
        ptsOut[i][0] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = (SCENE_WIDTH / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble());
        i++;

        // BOTTOM RIGHT
        ptsOut[i][0] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        // BOTTOM
        ptsOut[i][0] = (SCENE_WIDTH / 3 + (SCENE_WIDTH / 10) * RANDOM.nextDouble());
        ptsOut[i][1] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        // BOTTOM LEFT
        ptsOut[i][0] = -(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        ptsOut[i][1] = +(LARGE_X_COORDINATE + SCENE_WIDTH * RANDOM.nextDouble());
        i++;

        System.out.println("ptsOut: " + Arrays.deepToString(ptsOut));

        System.out.println("Max tests: " + (combPts.length * ptsIn.length * comb2PtsIn8.size()));

        // Set Marlin properties:
        /*
        -Dprism.marlin=true
        -Dprism.marlin.double=true
         */
        System.setProperty("prism.marlin.log", "true");

        System.setProperty("prism.marlin.subPixel_log2_X", "8");
        System.setProperty("prism.marlin.clip", "true");

//        System.setProperty("prism.marlin.clip.subdivider.minLength", "-1");
//        System.setProperty("prism.marlin.clip.subdivider.minLength", "100");
    }

    @Override
    public void start(Stage stage) {
        final Path p = new Path();
        p.setFill((TEST_FILL) ? Color.STEELBLUE : null);

        p.setStroke((TEST_STROKE) ? Color.RED : null);
        p.setStrokeWidth(4);
        p.setCache(false);

        final Scene scene = new Scene(new Group(p), SCENE_WIDTH, SCENE_WIDTH);
        stage.setTitle("PolylineWideClipTest");
        stage.setScene(scene);
        stage.show();

        final AnimationTimer anim = new AnimationTimer() {
            int numHandle = 0;
            int numTest = 0;
            int i = 0; // triangle permutation
            int j = 0; // in permutation
            int k = 0; // out permutation

            final double[][] pts = new double[3][];

            @Override
            public void handle(long now) {
                if ((numHandle++) % SPEED == 0) {
                    System.out.println("Test[" + numTest + "] i = " + i + " j = " + j + " k = " + k + " ----");

                    // 0 is inside
                    pts[0] = ptsIn[j];

                    final int[] cb = comb2PtsIn8.get(k);
                    pts[1] = ptsOut[cb[0]];
                    pts[2] = ptsOut[cb[1]];

                    /*
                    System.out.println("P0: "+Arrays.toString(pts[0]));
                    System.out.println("P1: "+Arrays.toString(pts[1]));
                    System.out.println("P2: "+Arrays.toString(pts[2]));
                     */
                    final int[] idxPt = combPts[i];

                    final ObservableList<PathElement> pathElements = p.getElements();
                    if (CLOSE) {
                        pathElements.setAll(
                                new MoveTo(pts[idxPt[0]][0], pts[idxPt[0]][1]),
                                new LineTo(pts[idxPt[1]][0], pts[idxPt[1]][1]),
                                new LineTo(pts[idxPt[2]][0], pts[idxPt[2]][1]),
                                new ClosePath()
                        );
                    } else {
                        pathElements.setAll(
                                new MoveTo(pts[idxPt[0]][0], pts[idxPt[0]][1]),
                                new LineTo(pts[idxPt[1]][0], pts[idxPt[1]][1]),
                                new LineTo(pts[idxPt[2]][0], pts[idxPt[2]][1])
                        );
                    }

                    numTest++;
                    if ((++i) >= combPts.length) {
                        i = 0;
                        if ((++j) >= ptsIn.length) {
                            j = 0;
                            if ((++k) >= comb2PtsIn8.size()) {
                                k = 0;
                                System.out.println("All tests done !");

                                p.setFill((TEST_FILL) ? Color.GREEN : null);
                                p.setStroke((TEST_STROKE) ? Color.GREEN : null);
                            }
                        }
                    }
                }
            }
        };
        anim.start();
    }

    public static void main(String argv[]) {
        launch(argv);
    }

    /**
     * Generate all combinations (no repetition, no ordering)
     * @param n number of elements
     * @param k number of items to choose
     * @return list of all combinations (integer arrays)
     */
    public static List<int[]> generateCombinations(final int n, final int k) {
        final int count = comb(n, k);

        final List<int[]> results = new ArrayList<int[]>(count);

        recursiveCombinations(n, k, results, new int[k], 0, 0);

        return results;
    }

    /**
     * Recursive algorithm to generate all combinations
     * @param n number of elements
     * @param k number of items to choose
     * @param results result array
     * @param current current array
     * @param position position in the array
     * @param nextInt next integer value
     */
    private static void recursiveCombinations(final int n, final int k, final List<int[]> results, final int[] current, final int position, final int nextInt) {
        for (int i = nextInt; i < n; i++) {
            current[position] = i;

            if (position + 1 == k) {
                // copy current result :
                final int[] res = new int[k];
                System.arraycopy(current, 0, res, 0, k);
                results.add(res);
            } else {
                recursiveCombinations(n, k, results, current, position + 1, i + 1);
            }
        }
    }

    /**
     * Return factorial(n) = n!
     * @param n integer
     * @return factorial(n)
     */
    public static int fact(final int n) {
        int res = 1;
        for (int i = 1; i <= n; i++) {
            res *= i;
        }
        return res;
    }

    /**
     * Return the number of arrangements without repetition
     * @param n number of elements
     * @param k number of items to choose
     * @return number of arrangements
     */
    public static int arr(final int n, final int k) {
        int res = 1;

        // A-n-k = n! / (n - k)!
        for (int i = n, min = n - k + 1; i >= min; i--) {
            res *= i;
        }
        return res;
    }

    /**
     * Return the number of combinations (no repetition, no ordering)
     * @param n number of elements
     * @param k number of items to choose
     * @return number of generateCombinations
     */
    public static int comb(final int n, final int k) {
        //C-n-k = A-n-k/k!
        return arr(n, k) / fact(k);
    }

}
