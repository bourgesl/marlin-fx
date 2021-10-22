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

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 */
public class PolygonTest extends Application {

    // (2^31 = 1073741824) / 256 = 4194304 => overflow in DRenderer
    private static final double LARGE_X_COORDINATE = 4194304.250; // -Dprism.marlin.subPixel_log2_X=8
//    private static final Double LARGE_X_COORDINATE = 134217728.250; // -Dprism.marlin.subPixel_log2_X=3
    
//    private static final double epsilon = 1.5E10; // max power of ten before translation into viewport is wrong
    private static final double epsilon = 1000;

    private static final double SCENE_WIDTH = 600.0;

    @Override
    public void start(Stage stage) {
        double dpi = Screen.getPrimary().getDpi();
        System.out.println("dpi: " + dpi);

        double dpiScale = 1.0; // Screen.getPrimary().getOutputScaleX();

        double longWidth = LARGE_X_COORDINATE / dpiScale + SCENE_WIDTH + 0.001 + epsilon;

        final Polygon veryWidePolygon;
        if (true) {
            if (true) {
                veryWidePolygon = new Polygon(
                        0.0, -1000.0,
                        100.0, -500.0,
                        longWidth, 200.0,
                        longWidth - 1000, 500.0
                );

            } else {
                // inverted test case => no large moveTo in Filler but bug in Stroker:
                if (true) {
                    veryWidePolygon = new Polygon(
                            0.0, 100.0,
                            0.0, 0.0,
                            longWidth, 50.0,
                            longWidth, 100.0
                    );
                } else {
                    veryWidePolygon = new Polygon(
                            longWidth, 50.0,
                            longWidth, 100.0,
                            0.0, 100.0,
                            0.0, 0.0
                    );
                }
            }
        } else {
            // original test case => large moveTo in Filler but no bug in Stroker:
            veryWidePolygon = new Polygon(
                    0.0, 0.0,
                    longWidth, 50.0,
                    longWidth, 100.0,
                    0.0, 100.0);
        }

        veryWidePolygon.setFill(Color.STEELBLUE);
        veryWidePolygon.setStroke(Color.RED);
        veryWidePolygon.setStrokeWidth(5);

        Group group = new Group(veryWidePolygon);
        group.getTransforms().add(new Translate(-longWidth + SCENE_WIDTH, 100.0));

        Scene scene = new Scene(group, SCENE_WIDTH, 400, Color.LIGHTGRAY);
        stage.setScene(scene);
        stage.setTitle("DPI scale: " + dpiScale);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
