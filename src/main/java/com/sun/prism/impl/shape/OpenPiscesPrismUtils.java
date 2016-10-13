/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.marlin.RendererContext;
import com.sun.openpisces.Dasher;
import com.sun.openpisces.Renderer;
import com.sun.openpisces.Stroker;
import com.sun.openpisces.TransformingPathConsumer2D;
import com.sun.prism.BasicStroke;
//import org.marlin.pisces.TransformingPathConsumer2D;

public class OpenPiscesPrismUtils {
    private static final Renderer savedAARenderer = new Renderer(3, 3);
    private static final Renderer savedRenderer = new Renderer(0, 0);
    private static final Stroker savedStroker = new Stroker(savedRenderer);
    private static final Dasher savedDasher = new Dasher(savedStroker);

    private static TransformingPathConsumer2D.FilterSet transformer =
        new TransformingPathConsumer2D.FilterSet();

    private static PathConsumer2D initRenderer(BasicStroke stroke,
                                               BaseTransform tx,
                                               Rectangle clip,
                                               int pirule,
                                               Renderer renderer)
    {
        int oprule = (stroke == null && pirule == PathIterator.WIND_EVEN_ODD) ?
            Renderer.WIND_EVEN_ODD : Renderer.WIND_NON_ZERO;
        renderer.reset(clip.x, clip.y, clip.width, clip.height, oprule);
        PathConsumer2D ret = transformer.getConsumer(renderer, tx);
        if (stroke != null) {
            savedStroker.reset(stroke.getLineWidth(), stroke.getEndCap(),
                               stroke.getLineJoin(), stroke.getMiterLimit());
            savedStroker.setConsumer(ret);
            ret = savedStroker;
            float dashes[] = stroke.getDashArray();
            if (dashes != null) {
                savedDasher.reset(dashes, stroke.getDashPhase());
                ret = savedDasher;
            }
        }
        return ret;
    }

    private static final RendererContext savedRendererContext = RendererContext.createContext();
    
    static {
        com.sun.marlin.MarlinRenderingEngine.logSettings(savedRendererContext.renderer.getClass().getName());
    }
    
    /*    
    private static final Renderer savedAARenderer = new Renderer(3, 3);
    private static final Renderer savedRenderer = new Renderer(0, 0);
    private static final Stroker savedStroker = new Stroker(savedRenderer);
    private static final Dasher savedDasher = new Dasher(savedStroker);

    private static TransformingPathConsumer2D.FilterSet transformer =
        new TransformingPathConsumer2D.FilterSet();
*/
    private static PathConsumer2D initMarlinRenderer(BasicStroke stroke,
                                               BaseTransform tx,
                                               Rectangle clip,
                                               int pirule,
                                               com.sun.marlin.Renderer renderer)
    {
        int oprule = (stroke == null && pirule == PathIterator.WIND_EVEN_ODD) ?
            com.sun.marlin.Renderer.WIND_EVEN_ODD : com.sun.marlin.Renderer.WIND_NON_ZERO;
        
        final RendererContext rdrCtx = savedRendererContext;

        rdrCtx.dispose();
/*
        // We use strokerat so that in Stroker and Dasher we can work only
        // with the pre-transformation coordinates. This will repeat a lot of
        // computations done in the path iterator, but the alternative is to
        // work with transformed paths and compute untransformed coordinates
        // as needed. This would be faster but I do not think the complexity
        // of working with both untransformed and transformed coordinates in
        // the same code is worth it.
        // However, if a path's width is constant after a transformation,
        // we can skip all this untransforming.

        // As pathTo() will check transformed coordinates for invalid values
        // (NaN / Infinity) to ignore such points, it is necessary to apply the
        // transformation before the path processing.
        BaseTransform strokerat = null;

        int dashLen = -1;
        boolean recycleDashes = false;

        if (tx != null && !tx.isIdentity()) {
            final double a = tx.getScaleX();
            final double b = tx.getShearX();
            final double c = tx.getShearY();
            final double d = tx.getScaleY();
            final double det = a * d - c * b;

            if (Math.abs(det) <= (2f * Float.MIN_VALUE)) {
                // this rendering engine takes one dimensional curves and turns
                // them into 2D shapes by giving them width.
                // However, if everything is to be passed through a singular
                // transformation, these 2D shapes will be squashed down to 1D
                // again so, nothing can be drawn.

                // Every path needs an initial moveTo and a pathDone. If these
                // are not there this causes a SIGSEGV in libawt.so (at the time
                // of writing of this comment (September 16, 2010)). Actually,
                // I am not sure if the moveTo is necessary to avoid the SIGSEGV
                // but the pathDone is definitely needed.
                pc2d.moveTo(0f, 0f);
                pc2d.pathDone();
                return;
            }

            // If the transform is a constant multiple of an orthogonal transformation
            // then every length is just multiplied by a constant, so we just
            // need to transform input paths to stroker and tell stroker
            // the scaled width. This condition is satisfied if
            // a*b == -c*d && a*a+c*c == b*b+d*d. In the actual check below, we
            // leave a bit of room for error.
            if (nearZero(a*b + c*d) && nearZero(a*a + c*c - (b*b + d*d))) {
                final float scale = (float) Math.sqrt(a*a + c*c);

                if (dashes != null) {
                    recycleDashes = true;
                    dashLen = dashes.length;
                    final float[] newDashes;
                    if (dashLen <= INITIAL_ARRAY) {
                        newDashes = rdrCtx.dasher.dashes_ref.initial;
                    } else {
                        if (DO_STATS) {
                            rdrCtx.stats.stat_array_dasher_dasher.add(dashLen);
                        }
                        newDashes = rdrCtx.dasher.dashes_ref.getArray(dashLen);
                    }
                    System.arraycopy(dashes, 0, newDashes, 0, dashLen);
                    dashes = newDashes;
                    for (int i = 0; i < dashLen; i++) {
                        dashes[i] *= scale;
                    }
                    dashphase *= scale;
                }
                width *= scale;

                // by now strokerat == null. Input paths to
                // stroker (and maybe dasher) will have the full transform at
                // applied to them and nothing will happen to the output paths.
            } else {
                strokerat = tx;

                // by now strokerat == tx. Input paths to
                // stroker (and maybe dasher) will have the full transform at
                // applied to them, then they will be normalized, and then
                // the inverse of *only the non translation part of tx* will
                // be applied to the normalized paths. This won't cause problems
                // in stroker, because, suppose tx = T*A, where T is just the
                // translation part of tx, and A is the rest. T*A has already
                // been applied to Stroker/Dasher's input. Then Ainv will be
                // applied. Ainv*T*A is not equal to T, but it is a translation,
                // which means that none of stroker's assumptions about its
                // input will be violated. After all this, A will be applied
                // to stroker's output.
            }
        } else {
            // either tx is null or it's the identity. In either case
            // we don't transform the path.
            tx = null;
        }



        final TransformingPathConsumer2D transformerPC2D = rdrCtx.transformerPC2D;
        pc2d = transformerPC2D.deltaTransformConsumer(pc2d, strokerat);

        pc2d = rdrCtx.stroker.init(pc2d, width, caps, join, miterlimit);

        if (dashes != null) {
            if (!recycleDashes) {
                dashLen = dashes.length;
            }
            pc2d = rdrCtx.dasher.init(pc2d, dashes, dashLen, dashphase,
                                      recycleDashes);
        }
        pc2d = transformerPC2D.inverseDeltaTransformConsumer(pc2d, strokerat);

        final PathIterator pi = norm.getNormalizingPathIterator(rdrCtx,
                                         src.getPathIterator(tx));

        pathTo(rdrCtx, pi, pc2d);
*/        

        /*
         * Pipeline seems to be:
         * shape.getPathIterator(tx)
         * -> (NormalizingPathIterator)
         * -> (inverseDeltaTransformConsumer)
         * -> (Dasher)
         * -> Stroker
         * -> (deltaTransformConsumer)
         *
         * -> (CollinearSimplifier) to remove redundant segments
         *
         * -> pc2d = Renderer (bounding box)
         */


        renderer.init(clip.x, clip.y, clip.width, clip.height, oprule);
        
// Try simple transformations like OpenPisces:
        PathConsumer2D ret = transformer.getConsumer(renderer, tx);
        
        if (stroke != null) {
            ret = rdrCtx.stroker.init(ret, stroke.getLineWidth(), stroke.getEndCap(),
                               stroke.getLineJoin(), stroke.getMiterLimit());
//            savedStroker.setConsumer(ret);
//            ret = savedStroker;
            float dashes[] = stroke.getDashArray();
            if (dashes != null) {
                ret = rdrCtx.dasher.init(ret, dashes, dashes.length, stroke.getDashPhase(), false);
//                ret = savedDasher;
            }
        }
        
//        System.out.println("initRenderer: " + ret);
        return ret;
    }

    public static void feedConsumer(PathIterator pi, PathConsumer2D pc) {
        float[] coords = new float[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    pc.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    pc.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    pc.quadTo(coords[0], coords[1],
                              coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    pc.curveTo(coords[0], coords[1],
                               coords[2], coords[3],
                               coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    pc.closePath();
                    break;
            }
            pi.next();
        }
        pc.pathDone();
    }

    // Pisces compatible for SW Java Renderer
    public static Renderer setupRenderer(Shape shape,
                                  BasicStroke stroke,
                                  BaseTransform xform,
                                  Rectangle rclip,
                                  boolean antialiasedShape)
    {
        PathIterator pi = shape.getPathIterator(null);
        Renderer r = antialiasedShape ? savedAARenderer : savedRenderer;
        feedConsumer(pi, initRenderer(stroke, xform, rclip, pi.getWindingRule(), r));
        return r;
    }
    
// TODO: create a new Renderer interface to expose getOutpixMin/Max[X/Y] + produceAlphas(AlphaConsumer)
    public static com.sun.marlin.Renderer setupMarlinRenderer(Shape shape,
                                  BasicStroke stroke,
                                  BaseTransform xform,
                                  Rectangle rclip,
                                  boolean antialiasedShape)
    {
        PathIterator pi = shape.getPathIterator(null); // xform for marlin ?
        com.sun.marlin.Renderer r = savedRendererContext.renderer;
//        Renderer r = antialiasedShape ? savedAARenderer : savedRenderer;
        feedConsumer(pi, initMarlinRenderer(stroke, xform, rclip, pi.getWindingRule(), r));
        return r;
    }

    public static com.sun.marlin.Renderer setupMarlinRenderer(Path2D p2d,
                                  BasicStroke stroke,
                                  BaseTransform xform,
                                  Rectangle rclip,
                                  boolean antialiasedShape)
    {
        com.sun.marlin.Renderer r = savedRendererContext.renderer;
//        Renderer r = antialiasedShape ? savedAARenderer : savedRenderer;
        PathConsumer2D pc2d = initMarlinRenderer(stroke, xform, rclip, p2d.getWindingRule(), r);

        float coords[] = p2d.getFloatCoordsNoClone();
        byte types[] = p2d.getCommandsNoClone();
        int nsegs = p2d.getNumCommands();
        int coff = 0;
        for (int i = 0; i < nsegs; i++) {
            switch (types[i]) {
                case PathIterator.SEG_MOVETO:
                    pc2d.moveTo(coords[coff+0], coords[coff+1]);
                    coff += 2;
                    break;
                case PathIterator.SEG_LINETO:
                    pc2d.lineTo(coords[coff+0], coords[coff+1]);
                    coff += 2;
                    break;
                case PathIterator.SEG_QUADTO:
                    pc2d.quadTo(coords[coff+0], coords[coff+1],
                                coords[coff+2], coords[coff+3]);
                    coff += 4;
                    break;
                case PathIterator.SEG_CUBICTO:
                    pc2d.curveTo(coords[coff+0], coords[coff+1],
                                 coords[coff+2], coords[coff+3],
                                 coords[coff+4], coords[coff+5]);
                    coff += 6;
                    break;
                case PathIterator.SEG_CLOSE:
                    pc2d.closePath();
                    break;
            }
        }
        pc2d.pathDone();
        return r;
    }
}
