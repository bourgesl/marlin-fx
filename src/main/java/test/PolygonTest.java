/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

    /*
     // -Dprism.marlin.subPixel_log2_X=8
     */
    // (2^31 = 1073741824) / 256 = 4194304 => overflow in DRenderer
    private static final double LARGE_X_COORDINATE = 4194304.250;
    private static final Double SCENE_WIDTH = 600.0;

    @Override
    public void start(Stage stage) {
        double dpi = Screen.getPrimary().getDpi();
        System.out.println("dpi: " + dpi);

        double dpiScale = Screen.getPrimary().getOutputScaleX();

        // original test case => large moveTo in Filler but no bug in Stroker:
        final Double longWidth = LARGE_X_COORDINATE / dpiScale + SCENE_WIDTH + 0.001;

        final Polygon veryWidePolygon;

        if (true) {
            veryWidePolygon = new Polygon(
                    longWidth, 50.0,
                    longWidth, 100.0,
                    0.0, 100.0,
                    0.0, 0.0
            );
        } else {
            veryWidePolygon = new Polygon(0.0, 0.0,
                    longWidth, 50.0,
                    longWidth, 100.0,
                    0.0, 100.0);

        }

        veryWidePolygon.setFill(Color.BLUE);
        veryWidePolygon.setStroke(Color.RED);
        veryWidePolygon.setStrokeWidth(2);

        Group group = new Group(veryWidePolygon);
        group.getTransforms().add(new Translate(-longWidth + SCENE_WIDTH, 100.0));

        Scene scene = new Scene(group, SCENE_WIDTH, 400, Color.WHITE);
        stage.setScene(scene);
        stage.setTitle("DPI scale: " + dpiScale);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
