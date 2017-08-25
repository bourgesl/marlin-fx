/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

public class PolylineClipTest extends Application {

    static final int NUM_OFFSCREEN = 10000;
    static boolean OMIT_OFFSCREEN = false;

    @Override
    public void start(Stage stage) {
        Path p = new Path();
        p.setStroke(Color.BLUE);
        p.setFill(null);

        if (OMIT_OFFSCREEN) {
            p.getElements().add(new MoveTo(-100, 100));
        } else {
            p.getElements().add(new MoveTo(-500, 100));
            for (int i = 0; i < NUM_OFFSCREEN; i++) {
                double x = Math.random() * 400 - 500;
                double y = Math.random() * 200;
                p.getElements().add(new LineTo(x, y));
            }
            p.getElements().add(new LineTo(-100, 100));
        }

        final LineTo lt1 = new LineTo(50, 150);
        final LineTo lt2 = new LineTo(150, 50);
        p.getElements().add(lt1);
        p.getElements().add(lt2);
        p.setCache(false);

        Scene scene = new Scene(new Group(p), 200, 200);
        stage.setScene(scene);
        stage.show();

        AnimationTimer anim = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double x1 = Math.random() * 20 + 40;
                double y1 = Math.random() * 20 + 140;
                double x2 = Math.random() * 20 + 140;
                double y2 = Math.random() * 20 + 40;
                lt1.setX(x1);
                lt1.setY(y1);
                lt2.setX(x2);
                lt2.setY(y2);
            }
        };
        anim.start();
    }

    public static void main(String argv[]) {
        OMIT_OFFSCREEN = (argv.length != 0);
        System.out.println("OMIT_OFFSCREEN: " + OMIT_OFFSCREEN);
        launch(argv);
    }
}
