/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.prism.impl.shape;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathConsumer2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.PrismSettings;

public class ShapeUtil {

    private static final boolean MARLIN_ENABLED;
    private static final boolean USE_MARLIN_DP;
    private static final ShapeRasterizer shapeRasterizer;
    static {
        // Enable Marlin-FX by setting -Dprism.marlin=true
        MARLIN_ENABLED = com.sun.marlin.MarlinProperties.isMarlinEnabled();
        USE_MARLIN_DP  = com.sun.marlin.MarlinProperties.isDoublePrecisionEnabled();

        if (MARLIN_ENABLED) {
            if (USE_MARLIN_DP) {
                System.out.println("Marlin-FX[" + com.sun.marlin.Version.getVersion() + "] (double) enabled.");
                shapeRasterizer = new DMarlinRasterizer();
            } else {
                System.out.println("Marlin-FX[" + com.sun.marlin.Version.getVersion() + "] (float)  enabled.");
                shapeRasterizer = new MarlinRasterizer();
            }
        } else if (PrismSettings.doNativePisces) {
            shapeRasterizer = new NativePiscesRasterizer();
        } else {
            shapeRasterizer = new OpenPiscesRasterizer();
        }
    }

    public static MaskData rasterizeShape(Shape shape,
                                          BasicStroke stroke,
                                          RectBounds xformBounds,
                                          BaseTransform xform,
                                          boolean close, boolean antialiasedShape)
    {
        return shapeRasterizer.getMaskData(shape, stroke, xformBounds, xform, close, antialiasedShape);
    }

    public static Shape createCenteredStrokedShape(Shape s, BasicStroke stroke)
    {
        if (MARLIN_ENABLED) {
            if (USE_MARLIN_DP) {
                return DMarlinRasterizer.createCenteredStrokedShape(s, stroke);
            }
            return MarlinRasterizer.createCenteredStrokedShape(s, stroke);
        }
        // JavaPisces fallback:
        return createCenteredStrokedShapeOpenPisces(s, stroke);
    }

    private static Shape createCenteredStrokedShapeOpenPisces(Shape s, BasicStroke stroke)
    {
        final float lw = (stroke.getType() == BasicStroke.TYPE_CENTERED) ?
                             stroke.getLineWidth() : stroke.getLineWidth() * 2.0f;

        final Path2D p2d = new Path2D(Path2D.WIND_NON_ZERO);

        PathConsumer2D pc2d =
            new com.sun.openpisces.Stroker(p2d, lw, stroke.getEndCap(),
                                                    stroke.getLineJoin(),
                                                    stroke.getMiterLimit());

        if (stroke.isDashed()) {
            pc2d = new com.sun.openpisces.Dasher(pc2d, stroke.getDashArray(),
                                                       stroke.getDashPhase());
        }
        com.sun.prism.impl.shape.OpenPiscesPrismUtils.feedConsumer(
                s.getPathIterator(null), pc2d);

        return p2d;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private ShapeUtil() {
    }
}
