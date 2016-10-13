/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.security.AccessController;
import static com.sun.marlin.MarlinUtils.logInfo;
import com.sun.javafx.geom.PathConsumer2D;
import com.sun.ReentrantContextProvider;
import com.sun.ReentrantContextProviderCLQ;
import com.sun.ReentrantContextProviderTL;
import sun.java2d.pipe.AATileGenerator;
import sun.java2d.pipe.Region;
import sun.security.action.GetPropertyAction;

/**
 * Marlin RendererEngine implementation (derived from Pisces)
 */
public class MarlinRenderingEngine implements MarlinConst
{
    private static final float MIN_PEN_SIZE = 1f / NORM_SUBPIXELS;

    static final float UPPER_BND = Float.MAX_VALUE / 2.0f;
    static final float LOWER_BND = -UPPER_BND;

    /**
     * Public constructor
     */
    public MarlinRenderingEngine() {
        super();
        logSettings(MarlinRenderingEngine.class.getName());
    }

    /**
     * Create a widened path as specified by the parameters.
     * <p>
     * The specified {@code src} {@link Shape} is widened according
     * to the specified attribute parameters as per the
     * {@link BasicStroke} specification.
     *
     * @param src the source path to be widened
     * @param width the width of the widened path as per {@code BasicStroke}
     * @param caps the end cap decorations as per {@code BasicStroke}
     * @param join the segment join decorations as per {@code BasicStroke}
     * @param miterlimit the miter limit as per {@code BasicStroke}
     * @param dashes the dash length array as per {@code BasicStroke}
     * @param dashphase the initial dash phase as per {@code BasicStroke}
     * @return the widened path stored in a new {@code Shape} object
     * @since 1.7
     */
    public Shape createStrokedShape(Shape src,
                                    float width,
                                    int caps,
                                    int join,
                                    float miterlimit,
                                    float[] dashes,
                                    float dashphase)
    {
        final RendererContext rdrCtx = getRendererContext();
        try {
            // initialize a large copyable Path2D to avoid a lot of array growing:
            final Path2D p2d = rdrCtx.getPath2D();

            strokeTo(rdrCtx,
                     src,
                     null,
                     width,
                     caps,
                     join,
                     miterlimit,
                     dashes,
                     dashphase,
                     null // rdrCtx.transformerPC2D.wrapPath2d(p2d)
                    );

            // Use Path2D copy constructor (trim)
            return new Path2D.Float(p2d);

        } finally {
            // recycle the RendererContext instance
            returnRendererContext(rdrCtx);
        }
    }

    /**
     * Sends the geometry for a widened path as specified by the parameters
     * to the specified consumer.
     * <p>
     * The specified {@code src} {@link Shape} is widened according
     * to the parameters specified by the {@link BasicStroke} object.
     * Adjustments are made to the path as appropriate for the
     * {@link VALUE_STROKE_NORMALIZE} hint if the {@code normalize}
     * boolean parameter is true.
     * Adjustments are made to the path as appropriate for the
     * {@link VALUE_ANTIALIAS_ON} hint if the {@code antialias}
     * boolean parameter is true.
     * <p>
     * The geometry of the widened path is forwarded to the indicated
     * {@link PathConsumer2D} object as it is calculated.
     *
     * @param src the source path to be widened
     * @param bs the {@code BasicSroke} object specifying the
     *           decorations to be applied to the widened path
     * @param normalize indicates whether stroke normalization should
     *                  be applied
     * @param antialias indicates whether or not adjustments appropriate
     *                  to antialiased rendering should be applied
     * @param consumer the {@code PathConsumer2D} instance to forward
     *                 the widened geometry to
     * @since 1.7
     */
    public void strokeTo(Shape src,
                         AffineTransform at,
                         BasicStroke bs,
                         boolean thin,
                         boolean normalize,
                         boolean antialias,
                         final PathConsumer2D consumer)
    {
        final RendererContext rdrCtx = getRendererContext();
        try {
            strokeTo(rdrCtx, src, at, bs, thin, antialias, consumer);
        } finally {
            // recycle the RendererContext instance
            returnRendererContext(rdrCtx);
        }
    }

    final void strokeTo(final RendererContext rdrCtx,
                        Shape src,
                        AffineTransform at,
                        BasicStroke bs,
                        boolean thin,
                        boolean antialias,
                        PathConsumer2D pc2d)
    {
        float lw;
        if (thin) {
            if (antialias) {
                lw = userSpaceLineWidth(at, MIN_PEN_SIZE);
            } else {
                lw = userSpaceLineWidth(at, 1.0f);
            }
        } else {
            lw = bs.getLineWidth();
        }
        strokeTo(rdrCtx,
                 src,
                 at,
                 lw,
                 bs.getEndCap(),
                 bs.getLineJoin(),
                 bs.getMiterLimit(),
                 bs.getDashArray(),
                 bs.getDashPhase(),
                 pc2d);
    }

    private final float userSpaceLineWidth(AffineTransform at, float lw) {

        float widthScale;

        if (at == null) {
            widthScale = 1.0f;
        } else if ((at.getType() & (AffineTransform.TYPE_GENERAL_TRANSFORM  |
                                    AffineTransform.TYPE_GENERAL_SCALE)) != 0) {
            widthScale = (float)Math.sqrt(at.getDeterminant());
        } else {
            // First calculate the "maximum scale" of this transform.
            double A = at.getScaleX();       // m00
            double C = at.getShearX();       // m01
            double B = at.getShearY();       // m10
            double D = at.getScaleY();       // m11

            /*
             * Given a 2 x 2 affine matrix [ A B ] such that
             *                             [ C D ]
             * v' = [x' y'] = [Ax + Cy, Bx + Dy], we want to
             * find the maximum magnitude (norm) of the vector v'
             * with the constraint (x^2 + y^2 = 1).
             * The equation to maximize is
             *     |v'| = sqrt((Ax+Cy)^2+(Bx+Dy)^2)
             * or  |v'| = sqrt((AA+BB)x^2 + 2(AC+BD)xy + (CC+DD)y^2).
             * Since sqrt is monotonic we can maximize |v'|^2
             * instead and plug in the substitution y = sqrt(1 - x^2).
             * Trigonometric equalities can then be used to get
             * rid of most of the sqrt terms.
             */

            double EA = A*A + B*B;          // x^2 coefficient
            double EB = 2.0*(A*C + B*D);    // xy coefficient
            double EC = C*C + D*D;          // y^2 coefficient

            /*
             * There is a lot of calculus omitted here.
             *
             * Conceptually, in the interests of understanding the
             * terms that the calculus produced we can consider
             * that EA and EC end up providing the lengths along
             * the major axes and the hypot term ends up being an
             * adjustment for the additional length along the off-axis
             * angle of rotated or sheared ellipses as well as an
             * adjustment for the fact that the equation below
             * averages the two major axis lengths.  (Notice that
             * the hypot term contains a part which resolves to the
             * difference of these two axis lengths in the absence
             * of rotation.)
             *
             * In the calculus, the ratio of the EB and (EA-EC) terms
             * ends up being the tangent of 2*theta where theta is
             * the angle that the long axis of the ellipse makes
             * with the horizontal axis.  Thus, this equation is
             * calculating the length of the hypotenuse of a triangle
             * along that axis.
             */

            double hypot = Math.sqrt(EB*EB + (EA-EC)*(EA-EC));
            // sqrt omitted, compare to squared limits below.
            double widthsquared = ((EA + EC + hypot)/2.0);

            widthScale = (float)Math.sqrt(widthsquared);
        }

        return (lw / widthScale);
    }

    final void strokeTo(final RendererContext rdrCtx,
                        Shape src,
                        AffineTransform at,
                        float width,
                        int caps,
                        int join,
                        float miterlimit,
                        float[] dashes,
                        float dashphase,
                        PathConsumer2D pc2d)
    {
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
        AffineTransform strokerat = null;

        int dashLen = -1;
        boolean recycleDashes = false;

        if (at != null && !at.isIdentity()) {
            final double a = at.getScaleX();
            final double b = at.getShearX();
            final double c = at.getShearY();
            final double d = at.getScaleY();
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
                strokerat = at;

                // by now strokerat == at. Input paths to
                // stroker (and maybe dasher) will have the full transform at
                // applied to them, then they will be normalized, and then
                // the inverse of *only the non translation part of at* will
                // be applied to the normalized paths. This won't cause problems
                // in stroker, because, suppose at = T*A, where T is just the
                // translation part of at, and A is the rest. T*A has already
                // been applied to Stroker/Dasher's input. Then Ainv will be
                // applied. Ainv*T*A is not equal to T, but it is a translation,
                // which means that none of stroker's assumptions about its
                // input will be violated. After all this, A will be applied
                // to stroker's output.
            }
        } else {
            // either at is null or it's the identity. In either case
            // we don't transform the path.
            at = null;
        }

        if (USE_SIMPLIFIER) {
            // Use simplifier after stroker before Renderer
            // to remove collinear segments (notably due to cap square)
            pc2d = rdrCtx.simplifier.init(pc2d);
        }

        final TransformingPathConsumer2D transformerPC2D = rdrCtx.transformerPC2D;
//        pc2d = transformerPC2D.deltaTransformConsumer(pc2d, strokerat);
        pc2d = transformerPC2D.deltaTransformConsumer(pc2d, null);

        pc2d = rdrCtx.stroker.init(pc2d, width, caps, join, miterlimit);

        if (dashes != null) {
            if (!recycleDashes) {
                dashLen = dashes.length;
            }
            pc2d = rdrCtx.dasher.init(pc2d, dashes, dashLen, dashphase,
                                      recycleDashes);
        }
//        pc2d = transformerPC2D.inverseDeltaTransformConsumer(pc2d, strokerat);
        pc2d = transformerPC2D.inverseDeltaTransformConsumer(pc2d, null);

        final PathIterator pi = src.getPathIterator(at);

        pathTo(rdrCtx, pi, pc2d);

        /*
         * Pipeline seems to be:
         * shape.getPathIterator(at)
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
    }

    private static boolean nearZero(final double num) {
        return Math.abs(num) < 2.0 * Math.ulp(num);
    }

    private static void pathTo(final RendererContext rdrCtx, final PathIterator pi,
                               final PathConsumer2D pc2d)
    {
        // mark context as DIRTY:
        rdrCtx.dirty = true;

        final float[] coords = rdrCtx.float6;

        pathToLoop(coords, pi, pc2d);

        // mark context as CLEAN:
        rdrCtx.dirty = false;
    }

    private static void pathToLoop(final float[] coords, final PathIterator pi,
                                   final PathConsumer2D pc2d)
    {
        // ported from DuctusRenderingEngine.feedConsumer() but simplified:
        // - removed skip flag = !subpathStarted
        // - removed pathClosed (ie subpathStarted not set to false)
        boolean subpathStarted = false;

        for (; !pi.isDone(); pi.next()) {
            switch (pi.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                /* Checking SEG_MOVETO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Skipping next path segment in case of
                 * invalid data.
                 */
                if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                    coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                {
                    pc2d.moveTo(coords[0], coords[1]);
                    subpathStarted = true;
                }
                break;
            case PathIterator.SEG_LINETO:
                /* Checking SEG_LINETO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid data. If segment is skipped its endpoint
                 * (if valid) is used to begin new subpath.
                 */
                if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                    coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                {
                    if (subpathStarted) {
                        pc2d.lineTo(coords[0], coords[1]);
                    } else {
                        pc2d.moveTo(coords[0], coords[1]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_QUADTO:
                // Quadratic curves take two points
                /* Checking SEG_QUADTO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid endpoints's data. Equivalent to the SEG_LINETO
                 * if endpoint coordinates are valid but there are invalid data
                 * among other coordinates
                 */
                if (coords[2] < UPPER_BND && coords[2] > LOWER_BND &&
                    coords[3] < UPPER_BND && coords[3] > LOWER_BND)
                {
                    if (subpathStarted) {
                        if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                            coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                        {
                            pc2d.quadTo(coords[0], coords[1],
                                        coords[2], coords[3]);
                        } else {
                            pc2d.lineTo(coords[2], coords[3]);
                        }
                    } else {
                        pc2d.moveTo(coords[2], coords[3]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_CUBICTO:
                // Cubic curves take three points
                /* Checking SEG_CUBICTO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid endpoints's data. Equivalent to the SEG_LINETO
                 * if endpoint coordinates are valid but there are invalid data
                 * among other coordinates
                 */
                if (coords[4] < UPPER_BND && coords[4] > LOWER_BND &&
                    coords[5] < UPPER_BND && coords[5] > LOWER_BND)
                {
                    if (subpathStarted) {
                        if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                            coords[1] < UPPER_BND && coords[1] > LOWER_BND &&
                            coords[2] < UPPER_BND && coords[2] > LOWER_BND &&
                            coords[3] < UPPER_BND && coords[3] > LOWER_BND)
                        {
                            pc2d.curveTo(coords[0], coords[1],
                                         coords[2], coords[3],
                                         coords[4], coords[5]);
                        } else {
                            pc2d.lineTo(coords[4], coords[5]);
                        }
                    } else {
                        pc2d.moveTo(coords[4], coords[5]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_CLOSE:
                if (subpathStarted) {
                    pc2d.closePath();
                    // do not set subpathStarted to false
                    // in case of missing moveTo() after close()
                }
                break;
            default:
            }
        }
        pc2d.pathDone();
    }

    /**
     * Construct an antialiased tile generator for the given shape with
     * the given rendering attributes and store the bounds of the tile
     * iteration in the bbox parameter.
     * The {@code at} parameter specifies a transform that should affect
     * both the shape and the {@code BasicStroke} attributes.
     * The {@code clip} parameter specifies the current clip in effect
     * in device coordinates and can be used to prune the data for the
     * operation, but the renderer is not required to perform any
     * clipping.
     * If the {@code BasicStroke} parameter is null then the shape
     * should be filled as is, otherwise the attributes of the
     * {@code BasicStroke} should be used to specify a draw operation.
     * The {@code thin} parameter indicates whether or not the
     * transformed {@code BasicStroke} represents coordinates smaller
     * than the minimum resolution of the antialiasing rasterizer as
     * specified by the {@code getMinimumAAPenWidth()} method.
     * <p>
     * Upon returning, this method will fill the {@code bbox} parameter
     * with 4 values indicating the bounds of the iteration of the
     * tile generator.
     * The iteration order of the tiles will be as specified by the
     * pseudo-code:
     * <pre>
     *     for (y = bbox[1]; y < bbox[3]; y += tileheight) {
     *         for (x = bbox[0]; x < bbox[2]; x += tilewidth) {
     *         }
     *     }
     * </pre>
     * If there is no output to be rendered, this method may return
     * null.
     *
     * @param s the shape to be rendered (fill or draw)
     * @param at the transform to be applied to the shape and the
     *           stroke attributes
     * @param clip the current clip in effect in device coordinates
     * @param bs if non-null, a {@code BasicStroke} whose attributes
     *           should be applied to this operation
     * @param thin true if the transformed stroke attributes are smaller
     *             than the minimum dropout pen width
     * @param normalize true if the {@code VALUE_STROKE_NORMALIZE}
     *                  {@code RenderingHint} is in effect
     * @param bbox returns the bounds of the iteration
     * @return the {@code AATileGenerator} instance to be consulted
     *         for tile coverages, or null if there is no output to render
     * @since 1.7
     */
    public AATileGenerator getAATileGenerator(Shape s,
                                              AffineTransform at,
                                              Region clip,
                                              BasicStroke bs,
                                              boolean thin,
                                              boolean normalize,
                                              int[] bbox)
    {
        MarlinTileGenerator ptg = null;
        Renderer r = null;

        final RendererContext rdrCtx = getRendererContext();
        try {
            // Test if at is identity:
            final AffineTransform _at = (at != null && !at.isIdentity()) ? at
                                        : null;

            if (bs == null) {
                // fill shape:
                final PathIterator pi = s.getPathIterator(_at);

                // note: Winding rule may be EvenOdd ONLY for fill operations !
                r = rdrCtx.renderer.init(clip.getLoX(), clip.getLoY(),
                                         clip.getWidth(), clip.getHeight(),
                                         pi.getWindingRule());

                // TODO: subdivide quad/cubic curves into monotonic curves ?
                pathTo(rdrCtx, pi, r);
            } else {
                // draw shape with given stroke:
                r = rdrCtx.renderer.init(clip.getLoX(), clip.getLoY(),
                                         clip.getWidth(), clip.getHeight(),
                                         PathIterator.WIND_NON_ZERO);

                strokeTo(rdrCtx, s, _at, bs, thin, true, r);
            }
            if (r.endRendering()) {
//                ptg = rdrCtx.ptg.init();
                ptg.getBbox(bbox);
                // note: do not returnRendererContext(rdrCtx)
                // as it will be called later by MarlinTileGenerator.dispose()
                r = null;
            }
        } finally {
            if (r != null) {
                // dispose renderer:
                r.dispose();
                // recycle the RendererContext instance
                MarlinRenderingEngine.returnRendererContext(rdrCtx);
            }
        }

        // Return null to cancel AA tile generation (nothing to render)
        return ptg;
    }

    static {
        if (PathIterator.WIND_NON_ZERO != Renderer.WIND_NON_ZERO ||
            PathIterator.WIND_EVEN_ODD != Renderer.WIND_EVEN_ODD ||
            BasicStroke.JOIN_MITER != Stroker.JOIN_MITER ||
            BasicStroke.JOIN_ROUND != Stroker.JOIN_ROUND ||
            BasicStroke.JOIN_BEVEL != Stroker.JOIN_BEVEL ||
            BasicStroke.CAP_BUTT != Stroker.CAP_BUTT ||
            BasicStroke.CAP_ROUND != Stroker.CAP_ROUND ||
            BasicStroke.CAP_SQUARE != Stroker.CAP_SQUARE)
        {
            throw new InternalError("mismatched renderer constants");
        }
    }

    // --- RendererContext handling ---
    // use ThreadLocal or ConcurrentLinkedQueue to get one RendererContext
    private static final boolean USE_THREAD_LOCAL;

    // reference type stored in either TL or CLQ
    static final int REF_TYPE;

    // Per-thread RendererContext
    private static final ReentrantContextProvider<RendererContext> RDR_CTX_PROVIDER;

    // Static initializer to use TL or CLQ mode
    static {
        USE_THREAD_LOCAL = MarlinProperties.isUseThreadLocal();

        // Soft reference by default:
        final String refType = AccessController.doPrivileged(
                            new GetPropertyAction("sun.java2d.renderer.useRef",
                            "soft"));

        // Java 1.6 does not support strings in switch:
        if ("hard".equalsIgnoreCase(refType)) {
            REF_TYPE = ReentrantContextProvider.REF_HARD;
        } else if ("weak".equalsIgnoreCase(refType)) {
            REF_TYPE = ReentrantContextProvider.REF_WEAK;
        } else {
            REF_TYPE = ReentrantContextProvider.REF_SOFT;
        }

        if (USE_THREAD_LOCAL) {
            RDR_CTX_PROVIDER = new ReentrantContextProviderTL<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        } else {
            RDR_CTX_PROVIDER = new ReentrantContextProviderCLQ<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        }
    }

    private static boolean SETTINGS_LOGGED = !ENABLE_LOGS;

    public static void logSettings(final String reClass) {
        // log information at startup
        if (SETTINGS_LOGGED) {
            return;
        }
        SETTINGS_LOGGED = true;

        String refType;
        switch (REF_TYPE) {
            default:
            case ReentrantContextProvider.REF_HARD:
                refType = "hard";
                break;
            case ReentrantContextProvider.REF_SOFT:
                refType = "soft";
                break;
            case ReentrantContextProvider.REF_WEAK:
                refType = "weak";
                break;
        }

        logInfo("=========================================================="
                + "=====================");

        logInfo("Marlin software rasterizer           = ENABLED");
        logInfo("Version                              = ["
                + Version.getVersion() + "]");
        logInfo("sun.java2d.renderer                  = "
                + reClass);
        logInfo("sun.java2d.renderer.useThreadLocal   = "
                + USE_THREAD_LOCAL);
        logInfo("sun.java2d.renderer.useRef           = "
                + refType);

        logInfo("sun.java2d.renderer.edges            = "
                + MarlinConst.INITIAL_EDGES_COUNT);
        logInfo("sun.java2d.renderer.pixelsize        = "
                + MarlinConst.INITIAL_PIXEL_DIM);

        logInfo("sun.java2d.renderer.subPixel_log2_X  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_X);
        logInfo("sun.java2d.renderer.subPixel_log2_Y  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_Y);
        
        logInfo("sun.java2d.renderer.tileSize_log2    = "
                + MarlinConst.TILE_H_LG);
        logInfo("sun.java2d.renderer.tileWidth_log2   = "
                + MarlinConst.TILE_W_LG);
        
        logInfo("sun.java2d.renderer.blockSize_log2   = "
                + MarlinConst.BLOCK_SIZE_LG);

        logInfo("sun.java2d.renderer.blockSize_log2   = "
                + MarlinConst.BLOCK_SIZE_LG);

        // RLE / blockFlags settings

        logInfo("sun.java2d.renderer.forceRLE         = "
                + MarlinProperties.isForceRLE());
        logInfo("sun.java2d.renderer.forceNoRLE       = "
                + MarlinProperties.isForceNoRLE());
        logInfo("sun.java2d.renderer.useTileFlags     = "
                + MarlinProperties.isUseTileFlags());
        logInfo("sun.java2d.renderer.useTileFlags.useHeuristics = "
                + MarlinProperties.isUseTileFlagsWithHeuristics());
        logInfo("sun.java2d.renderer.rleMinWidth      = "
                + MarlinCache.RLE_MIN_WIDTH);

        // optimisation parameters
        logInfo("sun.java2d.renderer.useSimplifier    = "
                + MarlinConst.USE_SIMPLIFIER);
        logInfo("sun.java2d.renderer.clip.curves      = "
                + MarlinProperties.isDoClipCurves());

        // debugging parameters
        logInfo("sun.java2d.renderer.doStats          = "
                + MarlinConst.DO_STATS);
        logInfo("sun.java2d.renderer.doMonitors       = "
                + MarlinConst.DO_MONITORS);
        logInfo("sun.java2d.renderer.doChecks         = "
                + MarlinConst.DO_CHECKS);

        // logging parameters
        logInfo("sun.java2d.renderer.useLogger        = "
                + MarlinConst.USE_LOGGER);
        logInfo("sun.java2d.renderer.logCreateContext = "
                + MarlinConst.LOG_CREATE_CONTEXT);
        logInfo("sun.java2d.renderer.logUnsafeMalloc  = "
                + MarlinConst.LOG_UNSAFE_MALLOC);

        // quality settings
        logInfo("Renderer settings:");
        logInfo("CUB_COUNT_LG = " + Renderer.CUB_COUNT_LG);
        logInfo("CUB_DEC_BND  = " + Renderer.CUB_DEC_BND);
        logInfo("CUB_INC_BND  = " + Renderer.CUB_INC_BND);
        logInfo("QUAD_DEC_BND = " + Renderer.QUAD_DEC_BND);

        logInfo("INITIAL_EDGES_CAPACITY               = "
                + MarlinConst.INITIAL_EDGES_CAPACITY);
        logInfo("INITIAL_CROSSING_COUNT               = "
                + Renderer.INITIAL_CROSSING_COUNT);

        logInfo("=========================================================="
                + "=====================");
    }

    /**
     * Get the RendererContext instance dedicated to the current thread
     * @return RendererContext instance
     */
    @SuppressWarnings({"unchecked"})
    static RendererContext getRendererContext() {
        final RendererContext rdrCtx = RDR_CTX_PROVIDER.acquire();
        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.start();
        }
        return rdrCtx;
    }

    /**
     * Reset and return the given RendererContext instance for reuse
     * @param rdrCtx RendererContext instance
     */
    static void returnRendererContext(final RendererContext rdrCtx) {
        rdrCtx.dispose();

        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.stop();
        }
        RDR_CTX_PROVIDER.release(rdrCtx);
    }
}
