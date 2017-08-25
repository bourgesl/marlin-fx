/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;


import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.marlin.DHelpers.PolyStack;

public final class DTransformingPathConsumer2D {

    private final DRendererContext rdrCtx;

    // recycled ClosedPathDetector instance from detectClosedPath()
    private final ClosedPathDetector   cpDetector;

    // recycled DPathConsumer2D instances from deltaTransformConsumer()
    private final DeltaScaleFilter     dt_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter dt_DeltaTransformFilter = new DeltaTransformFilter();

    // recycled DPathConsumer2D instances from inverseDeltaTransformConsumer()
    private final DeltaScaleFilter     iv_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter iv_DeltaTransformFilter = new DeltaTransformFilter();

    // recycled PathTracer instances from tracer...() methods
    private final PathTracer tracerInput      = new PathTracer("[Input]");
    private final PathTracer tracerCPDetector = new PathTracer("ClosedPathDetector");
    private final PathTracer tracerStroker    = new PathTracer("Stroker");

    DTransformingPathConsumer2D(final DRendererContext rdrCtx) {
        // used by RendererContext
        this.rdrCtx = rdrCtx;
        this.cpDetector = new ClosedPathDetector(rdrCtx);
    }

    public DPathConsumer2D traceInput(DPathConsumer2D out) {
        return tracerInput.init(out);
    }

    public DPathConsumer2D traceClosedPathDetector(DPathConsumer2D out) {
        return tracerCPDetector.init(out);
    }

    public DPathConsumer2D traceStroker(DPathConsumer2D out) {
        return tracerStroker.init(out);
    }

    public DPathConsumer2D detectClosedPath(DPathConsumer2D out)
    {
        return cpDetector.init(out);
    }

    public DPathConsumer2D deltaTransformConsumer(DPathConsumer2D out,
                                                  BaseTransform at,
                                                  final double rdrOffX,
                                                  final double rdrOffY)
    {
        if (at == null) {
            return out;
        }
        final double mxx = at.getMxx();
        final double mxy = at.getMxy();
        final double myx = at.getMyx();
        final double myy = at.getMyy();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                // Scale only
                if (rdrCtx.doClip) {
                    // adjust clip rectangle (ymin, ymax, xmin, xmax):
                    adjustClipOffset(rdrCtx.clipRect, rdrOffX, rdrOffY);
                    adjustClipScale(rdrCtx.clipRect, mxx, myy);
                }
                return dt_DeltaScaleFilter.init(out, mxx, myy);
            }
        } else {
            if (rdrCtx.doClip) {
                // adjust clip rectangle (ymin, ymax, xmin, xmax):
                adjustClipOffset(rdrCtx.clipRect, rdrOffX, rdrOffY);
                adjustClipInverseDelta(rdrCtx.clipRect, mxx, mxy, myx, myy);
            }
            return dt_DeltaTransformFilter.init(out, mxx, mxy, myx, myy);
        }
    }

    private static void adjustClipOffset(final double[] clipRect,
                                         final double rdrOffX,
                                         final double rdrOffY)
    {
        clipRect[0] += rdrOffY;
        clipRect[1] += rdrOffY;
        clipRect[2] += rdrOffX;
        clipRect[3] += rdrOffX;
    }

    private static void adjustClipScale(final double[] clipRect,
                                        final double mxx, final double myy)
    {
        // Adjust the clipping rectangle (iv_DeltaScaleFilter):
        clipRect[0] /= myy;
        clipRect[1] /= myy;
        clipRect[2] /= mxx;
        clipRect[3] /= mxx;
    }

    private static void adjustClipInverseDelta(final double[] clipRect,
                                               final double mxx, final double mxy,
                                               final double myx, final double myy)
    {
        // Adjust the clipping rectangle (iv_DeltaTransformFilter):
        final double det = mxx * myy - mxy * myx;
        final double imxx =  myy / det;
        final double imxy = -mxy / det;
        final double imyx = -myx / det;
        final double imyy =  mxx / det;

        double xmin, xmax, ymin, ymax;
        double x, y;
        // xmin, ymin:
        x = clipRect[2] * imxx + clipRect[0] * imxy;
        y = clipRect[2] * imyx + clipRect[0] * imyy;

        xmin = xmax = x;
        ymin = ymax = y;

        // xmax, ymin:
        x = clipRect[3] * imxx + clipRect[0] * imxy;
        y = clipRect[3] * imyx + clipRect[0] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        // xmin, ymax:
        x = clipRect[2] * imxx + clipRect[1] * imxy;
        y = clipRect[2] * imyx + clipRect[1] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        // xmax, ymax:
        x = clipRect[3] * imxx + clipRect[1] * imxy;
        y = clipRect[3] * imyx + clipRect[1] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        clipRect[0] = ymin;
        clipRect[1] = ymax;
        clipRect[2] = xmin;
        clipRect[3] = xmax;
    }

    public DPathConsumer2D inverseDeltaTransformConsumer(DPathConsumer2D out,
                                                        BaseTransform at)
    {
        if (at == null) {
            return out;
        }
        double mxx = at.getMxx();
        double mxy = at.getMxy();
        double myx = at.getMyx();
        double myy = at.getMyy();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                return iv_DeltaScaleFilter.init(out, 1.0d/mxx, 1.0d/myy);
            }
        } else {
            final double det = mxx * myy - mxy * myx;
            return iv_DeltaTransformFilter.init(out,
                                                myy / det,
                                               -mxy / det,
                                               -myx / det,
                                                mxx / det);
        }
    }

    static final class DeltaScaleFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double sx, sy;

        DeltaScaleFilter() {}

        DeltaScaleFilter init(DPathConsumer2D out,
                              double mxx, double myy)
        {
            this.out = out;
            sx = mxx;
            sy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * sx, y0 * sy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * sx, y1 * sy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * sx, y1 * sy,
                       x2 * sx, y2 * sy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * sx, y1 * sy,
                        x2 * sx, y2 * sy,
                        x3 * sx, y3 * sy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }
    }

    static final class DeltaTransformFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double mxx, mxy, myx, myy;

        DeltaTransformFilter() {}

        DeltaTransformFilter init(DPathConsumer2D out,
                                  double mxx, double mxy,
                                  double myx, double myy)
        {
            this.out = out;
            this.mxx = mxx;
            this.mxy = mxy;
            this.myx = myx;
            this.myy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * mxx + y0 * mxy,
                       x0 * myx + y0 * myy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy,
                       x2 * mxx + y2 * mxy,
                       x2 * myx + y2 * myy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * mxx + y1 * mxy,
                        x1 * myx + y1 * myy,
                        x2 * mxx + y2 * mxy,
                        x2 * myx + y2 * myy,
                        x3 * mxx + y3 * mxy,
                        x3 * myx + y3 * myy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }
    }

    static final class ClosedPathDetector implements DPathConsumer2D {

        private final DRendererContext rdrCtx;
        private final PolyStack stack;

        private DPathConsumer2D out;

        ClosedPathDetector(final DRendererContext rdrCtx) {
            this.rdrCtx = rdrCtx;
            this.stack = (rdrCtx.stats != null) ?
                new PolyStack(rdrCtx,
                        rdrCtx.stats.stat_cpd_polystack_types,
                        rdrCtx.stats.stat_cpd_polystack_curves,
                        rdrCtx.stats.hist_cpd_polystack_curves,
                        rdrCtx.stats.stat_array_cpd_polystack_curves,
                        rdrCtx.stats.stat_array_cpd_polystack_types)
                : new PolyStack(rdrCtx);
        }

        ClosedPathDetector init(DPathConsumer2D out) {
            this.out = out;
            return this; // fluent API
        }

        /**
         * Disposes this instance:
         * clean up before reusing this instance
         */
        void dispose() {
            stack.dispose();
        }

        @Override
        public void pathDone() {
            // previous path is not closed:
            finish(false);
            out.pathDone();

            // TODO: fix possible leak if exception happened
            // Dispose this instance:
            dispose();
        }

        @Override
        public void closePath() {
            // path is closed
            finish(true);
            out.closePath();
        }

        @Override
        public void moveTo(double x0, double y0) {
            // previous path is not closed:
            finish(false);
            out.moveTo(x0, y0);
        }

        private void finish(final boolean closed) {
            rdrCtx.closedPath = closed;
            stack.pullAll(out);
        }

        @Override
        public void lineTo(double x1, double y1) {
            stack.pushLine(x1, y1);
        }

        @Override
        public void curveTo(double x3, double y3,
                            double x2, double y2,
                            double x1, double y1)
        {
            stack.pushCubic(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void quadTo(double x2, double y2, double x1, double y1) {
            stack.pushQuad(x1, y1, x2, y2);
        }
    }

    static final class PathTracer implements DPathConsumer2D {
        private final String prefix;
        private DPathConsumer2D out;

        PathTracer(String name) {
            this.prefix = name + ": ";
        }

        PathTracer init(DPathConsumer2D out) {
            this.out = out;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            log("moveTo (" + x0 + ", " + y0 + ')');
            out.moveTo(x0, y0);
        }

        @Override
        public void lineTo(double x1, double y1) {
            log("lineTo (" + x1 + ", " + y1 + ')');
            out.lineTo(x1, y1);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            log("curveTo P1(" + x1 + ", " + y1 + ") P2(" + x2 + ", " + y2  + ") P3(" + x3 + ", " + y3 + ')');
            out.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void quadTo(double x1, double y1, double x2, double y2) {
            log("quadTo P1(" + x1 + ", " + y1 + ") P2(" + x2 + ", " + y2  + ')');
            out.quadTo(x1, y1, x2, y2);
        }

        @Override
        public void closePath() {
            log("closePath");
            out.closePath();
        }

        @Override
        public void pathDone() {
            log("pathDone");
            out.pathDone();
        }

        private void log(final String message) {
            System.out.println(prefix + message);
        }
    }
}
