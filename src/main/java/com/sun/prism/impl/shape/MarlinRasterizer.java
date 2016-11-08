/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.marlin.FloatMath;
import com.sun.marlin.IntArrayCache;
import com.sun.marlin.MarlinAlphaConsumer;
import com.sun.marlin.MarlinConst;
import static com.sun.marlin.MarlinConst.BLOCK_SIZE_LG;
import com.sun.marlin.MarlinRenderer;
import com.sun.marlin.MarlinRenderingEngine;
import com.sun.marlin.OffHeapArray;
import com.sun.marlin.RendererContext;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.PrismSettings;
import java.nio.ByteBuffer;
import java.util.Arrays;
import sun.misc.Unsafe;

/**
 * Thread-safe Marlin rasterizer (TL or CLQ storage)
 */
public final class MarlinRasterizer implements ShapeRasterizer {
    private static final MaskData EMPTY_MASK = MaskData.create(new byte[1], 0, 0, 1, 1);

    @Override
    public MaskData getMaskData(Shape shape,
                                BasicStroke stroke,
                                RectBounds xformBounds,
                                BaseTransform xform,
                                boolean close, boolean antialiasedShape)
    {
        if (stroke != null && stroke.getType() != BasicStroke.TYPE_CENTERED) {
            // RT-27427
            // TODO: Optimize the combinatorial strokes for simple
            // shapes and/or teach the rasterizer to be able to
            // do a "differential fill" between two shapes.
            // Note that most simple shapes will use a more optimized path
            // than this method for the INNER/OUTER strokes anyway.
            shape = stroke.createStrokedShape(shape);
            stroke = null;
        }
        if (xformBounds == null) {
            if (stroke != null) {
                // Note that all places that pass null for xformbounds also
                // pass null for stroke so that the following is not typically
                // executed, but just here as a safety net.
                shape = stroke.createStrokedShape(shape);
                stroke = null;
            }

            xformBounds = new RectBounds();
            //TODO: Need to verify that this is a safe cast ... (RT-27427)
            xformBounds = (RectBounds) xform.transform(shape.getBounds(), xformBounds);
        }
        if (xformBounds.isEmpty()) {
            return EMPTY_MASK;
        }

        final RendererContext rdrCtx = MarlinRenderingEngine.getRendererContext();
        MarlinRenderer renderer = null;
        try {
            final Rectangle rclip = rdrCtx.clip;
            rclip.setBounds(xformBounds);

            if (shape instanceof Path2D) {
                renderer = MarlinPrismUtils.setupRenderer(rdrCtx, (Path2D) shape, stroke, xform, rclip,
                        antialiasedShape);
            }
            if (renderer == null) {
                renderer = MarlinPrismUtils.setupRenderer(rdrCtx, shape, stroke, xform, rclip,
                        antialiasedShape);
            }

            final int outpix_ymin = renderer.getOutpixMinY();
            final int outpix_ymax = renderer.getOutpixMaxY();
            final int h = outpix_ymax - outpix_ymin;
            if (h <= 0) {
                return EMPTY_MASK;
            }
            final int outpix_xmin = renderer.getOutpixMinX();
            final int outpix_xmax = renderer.getOutpixMaxX();
            final int w = outpix_xmax - outpix_xmin;
            if (w <= 0) {
                return EMPTY_MASK;
            }

            Consumer consumer = (Consumer)rdrCtx.consumer;
            if (consumer == null || (w * h) > consumer.getAlphaLength()) {
                final int csize = (w * h + 0xfff) & (~0xfff);
                rdrCtx.consumer = consumer = new Consumer(csize);
                if (PrismSettings.verbose) {
                    System.out.println("new alphas with length = " + csize);
                }
            }
            consumer.setBoundsNoClone(outpix_xmin, outpix_ymin, w, h);
            renderer.produceAlphas(consumer);
            return consumer.getMaskData();
        } finally {
            if (renderer != null) {
                renderer.dispose();
            }
            // recycle the RendererContext instance
            MarlinRenderingEngine.returnRendererContext(rdrCtx);
        }
    }

    private static final class Consumer implements MarlinAlphaConsumer {
        int x, y, width, height;
        final byte alphas[];
        final ByteBuffer alphabuffer;
        final MaskData maskdata = new MaskData();

        boolean useFastFill;
        int fastFillThreshold;

        public Consumer(int alphalen) {
            this.alphas = new byte[alphalen];
            alphabuffer = ByteBuffer.wrap(alphas);
        }

        public void setBoundsNoClone(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            maskdata.update(alphabuffer, x, y, w, h);

            useFastFill = (w >= 32);
            if (useFastFill) {
                fastFillThreshold = (w >= 128) ? (w >> 1) : (w >> 2);
            }
        }

        @Override
        public int getOriginX() {
            return x;
        }

        @Override
        public int getOriginY() {
            return y;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        public byte[] getAlphasNoClone() {
            return alphas;
        }

        public int getAlphaLength() {
            return alphas.length;
        }

        public MaskData getMaskData() {
            return maskdata;
        }

        OffHeapArray ALPHA_MAP_USED = null;

        @Override
        public void setMaxAlpha(int maxalpha) {
            ALPHA_MAP_USED = (maxalpha == 1) ? ALPHA_MAP_UNSAFE_NO_AA : ALPHA_MAP_UNSAFE;
        }

        // The alpha map used by this object (taken out of our map cache) to convert
        // pixel coverage counts gotten from MarlinCache (which are in the range
        // [0, maxalpha]) into alpha values, which are in [0,256).
        static final byte[] ALPHA_MAP;
        static final OffHeapArray ALPHA_MAP_UNSAFE;

        static final byte[] ALPHA_MAP_NO_AA;
        static final OffHeapArray ALPHA_MAP_UNSAFE_NO_AA;

        static {
            final Unsafe _unsafe = OffHeapArray.UNSAFE;

            // AA:
            byte[] _ALPHA_MAP = buildAlphaMap(MarlinConst.MAX_AA_ALPHA);
            ALPHA_MAP = _ALPHA_MAP; // Keep alive the OffHeapArray
            ALPHA_MAP_UNSAFE = new OffHeapArray(ALPHA_MAP, ALPHA_MAP.length); // 1K

            long addr = ALPHA_MAP_UNSAFE.address;

            for (int i = 0; i < _ALPHA_MAP.length; i++) {
                _unsafe.putByte(addr + i, _ALPHA_MAP[i]);
            }

            // NoAA:
            byte[] _ALPHA_MAP_NO_AA = buildAlphaMap(1);
            ALPHA_MAP_NO_AA = _ALPHA_MAP_NO_AA; // Keep alive the OffHeapArray
            ALPHA_MAP_UNSAFE_NO_AA = new OffHeapArray(ALPHA_MAP_NO_AA, ALPHA_MAP_NO_AA.length);

            addr = ALPHA_MAP_UNSAFE_NO_AA.address;

            for (int i = 0; i < _ALPHA_MAP_NO_AA.length; i++) {
                _unsafe.putByte(addr + i, _ALPHA_MAP_NO_AA[i]);
            }
        }

        private static byte[] buildAlphaMap(final int maxalpha) {
            final byte[] alMap = new byte[maxalpha << 1];
            final int halfmaxalpha = maxalpha >> 2;
            for (int i = 0; i <= maxalpha; i++) {
                alMap[i] = (byte) ((i * 255 + halfmaxalpha) / maxalpha);
//            System.out.println("alphaMap[" + i + "] = "
//                               + Byte.toUnsignedInt(alMap[i]));
            }
            return alMap;
        }

        @Override
        public boolean supportBlockFlags() {
            return true;
        }

        @Override
        public void clearAlphas(final int pix_y) {
            final int w = width;
            final int off = (pix_y - y) * w;

            // Clear complete row:
           Arrays.fill(this.alphas, off, off + w, (byte)0);
        }

        @Override
        public void setAndClearRelativeAlphas(final int[] alphaDeltas, final int pix_y,
                                              final int pix_from, final int pix_to)
        {
//            System.out.println("setting row "+(pix_y - y)+
//                               " out of "+width+" x "+height);

            final byte out[] = this.alphas;
            final int w = width;
            final int off = (pix_y - y) * w;

            final Unsafe _unsafe = OffHeapArray.UNSAFE;
            final long addr_alpha = ALPHA_MAP_USED.address;

            final int from = pix_from - x;

            // skip useless pixels above boundary
            final int to = pix_to - x;
            final int ato = Math.min(to, width);

            // fast fill ?
            final boolean fast = useFastFill && ((ato - from) < fastFillThreshold);

            if (fast) {
                // Zero-fill complete row:
                Arrays.fill(out, off, off + w, (byte) 0);

                int i = from;
                int curAlpha = 0;

                while (i < ato) {
                    curAlpha += alphaDeltas[i];

                    out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]
                    i++;
                }

            } else {
                int i = 0;

                while (i < from) {
                    out[off + i] = 0;
                    i++;
                }

                int curAlpha = 0;

                while (i < ato) {
                    curAlpha += alphaDeltas[i];

                    out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]
                    i++;
                }

                while (i < w) {
                    out[off + i] = 0;
                    i++;
                }
            }

            // Clear alpha row for reuse:
            IntArrayCache.fill(alphaDeltas, from, to + 1, 0);
        }

        @Override
        public void setAndClearRelativeAlphas(final int[] blkFlags, final int[] alphaDeltas, final int pix_y,
                                              final int pix_from, final int pix_to)
        {
//            System.out.println("setting row "+(pix_y - y)+
//                               " out of "+width+" x "+height);

            final byte out[] = this.alphas;
            final int w = width;
            final int off = (pix_y - y) * w;

            final Unsafe _unsafe = OffHeapArray.UNSAFE;
            final long addr_alpha = ALPHA_MAP_USED.address;

            final int from = pix_from - x;

            // skip useless pixels above boundary
            final int to = pix_to - x;
            final int ato = Math.min(to, width);

            // fast fill ?
            final boolean fast = useFastFill && ((ato - from) < fastFillThreshold);

            final int _BLK_SIZE_LG  = BLOCK_SIZE_LG;

            // traverse flagged blocks:
            final int blkW = (from >> _BLK_SIZE_LG);
            final int blkE = (ato   >> _BLK_SIZE_LG) + 1;

            // Perform run-length encoding and store results in the piscesCache
            int curAlpha = 0;

            final int _MAX_VALUE = Integer.MAX_VALUE;
            int last_t0 = _MAX_VALUE;
            byte val;

            if (fast) {
                int i = from;

                // Zero-fill complete row:
                Arrays.fill(out, off, off + w, (byte) 0);

                for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
                    if (blkFlags[t] != 0) {
                        blkFlags[t] = 0;

                        if (last_t0 == _MAX_VALUE) {
                            last_t0 = t;
                        }
                        continue;
                    }
                    if (last_t0 != _MAX_VALUE) {
                        // emit blocks:
                        blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                        last_t0 = _MAX_VALUE;

                        // (last block pixel+1) inclusive => +1
                        blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, ato);

                        for (cx = blk_x0; cx < blk_x1; cx++) {
                            if ((delta = alphaDeltas[cx]) != 0) {
                                alphaDeltas[cx] = 0;

                                // fill span:
                                if (cx != i) {
                                    // skip alpha = 0
                                    if (curAlpha == 0) {
                                        i = cx;
                                    } else {
                                        val = _unsafe.getByte(addr_alpha + curAlpha);

                                        do {
                                            out[off + i] = val;
                                            i++;
                                        } while (i < cx);
                                    }
                                }

                                // alpha value = running sum of coverage delta:
                                curAlpha += delta;
                            }
                        }
                    }
                }

                // Process remaining span:
                val = _unsafe.getByte(addr_alpha + curAlpha);

                do {
                    out[off + i] = val;
                    i++;
                } while (i < ato);

            } else {
                int i = 0;

                while (i < from) {
                    out[off + i] = 0;
                    i++;
                }

                for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
                    if (blkFlags[t] != 0) {
                        blkFlags[t] = 0;

                        if (last_t0 == _MAX_VALUE) {
                            last_t0 = t;
                        }
                        continue;
                    }
                    if (last_t0 != _MAX_VALUE) {
                        // emit blocks:
                        blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                        last_t0 = _MAX_VALUE;

                        // (last block pixel+1) inclusive => +1
                        blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, ato);

                        for (cx = blk_x0; cx < blk_x1; cx++) {
                            if ((delta = alphaDeltas[cx]) != 0) {
                                alphaDeltas[cx] = 0;

                                // fill span:
                                if (cx != i) {
                                    val = _unsafe.getByte(addr_alpha + curAlpha);

                                    do {
                                        out[off + i] = val;
                                        i++;
                                    } while (i < cx);
                                }

                                // alpha value = running sum of coverage delta:
                                curAlpha += delta;
                            }
                        }
                    }
                }

                // Process remaining span:
                val = _unsafe.getByte(addr_alpha + curAlpha);

                do {
                    out[off + i] = val;
                    i++;
                } while (i < ato);

                while (i < w) {
                    out[off + i] = 0;
                    i++;
                }
            }

            // Clear alpha row for reuse:
            alphaDeltas[ato] = 0;

            if (MarlinConst.DO_CHECKS) {
                IntArrayCache.check(blkFlags, blkW, blkE, 0);
                IntArrayCache.check(alphaDeltas, from, to + 1, 0);
            }
        }
    }
}
