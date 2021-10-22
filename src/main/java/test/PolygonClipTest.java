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

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/**
 * Test dedicated to study special cases in Path clipper
 */
public class PolygonClipTest extends Application {

    private static final boolean TEST_STROKE = false;
    private static final int TEST_CASE = 4;

    private static final int SPEED = 60;

    private static final double SCENE_WIDTH = 100.0;
    private static final double SCALE = 3.0;

    static {
        System.setProperty("prism.marlin.subPixel_log2_X", "8");

        // disable static clipping setting:
        System.setProperty("prism.marlin.clip", "false");
        System.setProperty("prism.marlin.clip.runtime.enable", "true");

        // disable min length check: always subdivide curves at clip edges
        //System.setProperty("prism.marlin.clip.subdivider.minLength", "-1");
        if (false) {
            System.setProperty("prism.marlin.clip.subdivider.minLength", "100");
        } else {
            System.setProperty("prism.marlin.clip.subdivider.minLength", "-1");
        }
    }

    @Override
    public void start(Stage stage) {
        final Path p = new Path();
        p.setFill(Color.STEELBLUE);
        if (TEST_STROKE) {
            p.setStroke(Color.RED);
            p.setStrokeWidth(1.0 / SCALE);
        } else {
            p.setStroke(null);
        }
        p.setCache(false);
        p.setSmooth(true);

        final Group group = new Group(p);
        if (SCALE != 1.0) {
            group.getTransforms().add(new Scale(SCALE, SCALE));
        }

        final Scene scene = new Scene(group, SCALE * SCENE_WIDTH, SCALE * SCENE_WIDTH, Color.LIGHTGRAY);
        stage.setScene(scene);
        stage.setTitle(this.getClass().getSimpleName());
        stage.show();

        final AnimationTimer anim = new AnimationTimer() {
            int numHandle = 0;
            boolean clip = false;

            @Override
            public void handle(long now) {
                if ((numHandle++) % SPEED == 0) {
                    clip = !clip;
                    System.out.println("clip: " + clip);

                    // Enable or Disable clipping:
                    System.setProperty("prism.marlin.clip.runtime", (clip) ? "true" : "false");

                    p.setFill((clip) ? Color.BLUE : Color.GREEN);
                    updatePath(p);
                }
            }
        };
        anim.start();
    }

    private static void updatePath(final Path p) {
        // modifying path for every test ensures no caching (bounds...)
        switch (TEST_CASE) {
            case 1:
                p.getElements().setAll(
                        new MoveTo(-89.687675, 171.33438),
                        new LineTo(112.86113, 151.63348),
                        new LineTo(84.55856, 111.90166),
                        new LineTo(-2.169856, -22.49296),
                        new LineTo(155.98148, -65.42218),
                        new LineTo(100.07923, 96.86355),
                        new ClosePath()
                );
                break;
            case 2:
                p.getElements().setAll(
                        new MoveTo(-57.112507, 79.05166),
                        new LineTo(136.05696, 26.617828),
                        new LineTo(92.853615, 62.25588),
                        new LineTo(59.66546, 56.429977),
                        new LineTo(79.59174, -54.074516),
                        new LineTo(-75.93232, 39.502163),
                        new ClosePath(),
                        new LineTo(86.28725, -18.620544),
                        new LineTo(87.534134, -69.6632),
                        new LineTo(-61.609844, -46.943455),
                        new LineTo(-53.029644, -40.836628),
                        new LineTo(89.01727, -43.499767),
                        new ClosePath()
                );
                break;
            case 3:
                p.getElements().setAll(
                        new MoveTo(174.8631, 124.340775),
                        new LineTo(-13.485423, 120.01353),
                        new LineTo(-40.214275, -11.351073),
                        new LineTo(96.66595, 33.508484),
                        new LineTo(-31.891193, -17.238123),
                        new LineTo(-0.092007555, 49.85812),
                        new ClosePath(),
                        new LineTo(-35.58541, 126.748764),
                        new LineTo(13.534866, 105.12724),
                        new LineTo(-84.706535, 165.25713),
                        new LineTo(105.69439, 48.8846),
                        new LineTo(-34.655937, 88.94304),
                        new ClosePath()
                );
                break;
            case 4:
                p.getElements().setAll(
                        new MoveTo(-99.77336, 35.190475),
                        new CubicCurveTo(-25.539629, 180.36601, 52.512184, 42.104904, -66.391945, -7.1875143),
                        new CubicCurveTo(97.41586, 79.37796, 102.07544, 10.436856, -7.376722, 18.136734)
                );
                break;
            default:
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
