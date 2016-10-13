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
import com.sun.marlin.IntArrayCache;
import com.sun.marlin.MarlinConst;
import com.sun.marlin.OffHeapArray;
import com.sun.marlin.Renderer;
import com.sun.openpisces.AlphaConsumer;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.PrismSettings;
import java.nio.ByteBuffer;
import java.util.Arrays;
import sun.misc.Unsafe;

public class OpenPiscesRasterizer implements ShapeRasterizer {
    private static MaskData emptyData = MaskData.create(new byte[1], 0, 0, 1, 1);

    private static Consumer savedConsumer;

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
        Rectangle rclip = new Rectangle(xformBounds);
        if (rclip.isEmpty()) {
            return emptyData;
        }
        Renderer renderer = null;
        if (shape instanceof Path2D) {
            renderer = OpenPiscesPrismUtils.setupMarlinRenderer((Path2D) shape, stroke, xform, rclip,
                    antialiasedShape);
        }
        if (renderer == null) {
            renderer = OpenPiscesPrismUtils.setupMarlinRenderer(shape, stroke, xform, rclip,
                    antialiasedShape);
        }
        // TODO: make it inlined in Renderer.pathDone() to determine the boundaries only:
        renderer.endRendering();
        
        int outpix_xmin = renderer.getOutpixMinX();
        int outpix_ymin = renderer.getOutpixMinY();
        int outpix_xmax = renderer.getOutpixMaxX();
        int outpix_ymax = renderer.getOutpixMaxY();
        int w = outpix_xmax - outpix_xmin;
        int h = outpix_ymax - outpix_ymin;
        if (w <= 0 || h <= 0) {
            return emptyData;
        }

        Consumer consumer = savedConsumer;
        if (consumer == null || w * h > consumer.getAlphaLength()) {
            int csize = (w * h + 0xfff) & (~0xfff);
            savedConsumer = consumer = new Consumer(csize);
            
            if (PrismSettings.verbose) {
                System.out.println("new alphas with length = " + csize);
            }
        }
        consumer.setBoundsNoClone(outpix_xmin, outpix_ymin, w, h);
        renderer.produceAlphas(consumer);
        return consumer.getMaskData();
    }

    private static final class Consumer implements AlphaConsumer {
//        static byte savedAlphaMap[];
        int x, y, width, height;
        final byte alphas[];
//        byte alphaMap[];
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
//                fastFillThreshold = (w >> 2);
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

        @Override
        public void setMaxAlpha(int maxalpha) {
/*            
            byte map[] = savedAlphaMap;
            if (map == null || map.length != maxalpha+1) {
                // System.out.println("setMaxAlpha: recompute for maxalpha= "+maxalpha);
                map = new byte[maxalpha + 1];
                for (int i = 0; i <= maxalpha; i++) {
                    map[i] = (byte) ((i*255 + maxalpha/2)/maxalpha);
                }
                savedAlphaMap = map;
            }
            this.alphaMap = map;
*/            
        }

        // The alpha map used by this object (taken out of our map cache) to convert
        // pixel coverage counts gotten from MarlinCache (which are in the range
        // [0, maxalpha]) into alpha values, which are in [0,256).
        static final byte[] ALPHA_MAP;

        static final OffHeapArray ALPHA_MAP_UNSAFE;

        static {
            final byte[] _ALPHA_MAP = buildAlphaMap(MarlinConst.MAX_AA_ALPHA);

            ALPHA_MAP_UNSAFE = new OffHeapArray(_ALPHA_MAP, _ALPHA_MAP.length); // 1K
            ALPHA_MAP = _ALPHA_MAP;

            final Unsafe _unsafe = OffHeapArray.UNSAFE;
            final long addr = ALPHA_MAP_UNSAFE.address;

            for (int i = 0; i < _ALPHA_MAP.length; i++) {
                _unsafe.putByte(addr + i, _ALPHA_MAP[i]);
            }
        }

        private static byte[] buildAlphaMap(final int maxalpha) {
            // double size !
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
        public void setAndClearRelativeAlphas(int[] alphaDeltas, int pix_y,
                                              int pix_from, int pix_to)
        {
//            System.out.println("setting row "+(pix_y - y)+
//                               " out of "+width+" x "+height);
/*            
            int w = width;
            int off = (pix_y - y) * w;
            byte out[] = this.alphas;
            byte map[] = this.alphaMap;
            int a = 0;
            for (int i = 0; i < w; i++) {
                a += alphaRow[i];
                alphaRow[i] = 0;
                out[off+i] = map[a];
            }
        }

        public void setAndClearRelativeAlphas2(int[] alphaDeltas, int pix_y,
                                               int pix_from, int pix_to)
        {
*/
            final byte out[] = this.alphas;
            final int w = width;
            final int off = (pix_y - y) * w;
                
            if (pix_to > pix_from) {
                final Unsafe _unsafe = OffHeapArray.UNSAFE;
                final long addr_alpha = ALPHA_MAP_UNSAFE.address;
                
//                final byte map[] = this.alphaMap;
                final int from = pix_from - x;
                
                // skip useless pixels above boundary
                final int to = pix_to - x;
                final int ato = Math.min(to, width);
//                int to = pix_to - x;

                // fast fill ?
                final boolean fast = useFastFill && ((ato - from) < fastFillThreshold);
                
                if (fast) {
                    // System.out.println("dist: "+ (ato - from) + " << " + w);

                    // Zero-fill complete row:
                    Arrays.fill(out, off, off + w, (byte) 0);

                    int i = from;
                    int curAlpha = 0;
                    
                    while (i < ato) {
                        curAlpha += alphaDeltas[i];
//                    alphaDeltas[i] = 0;

//                        if (curAlpha != 0) {
                            // TODO: use Unsafe like MarlinCache
                            out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]                            
//                            out[off + i] = map[curAlpha];
//                        }
                        i++;
                    }
//                alphaDeltas[i] = 0;

                } else {
// TODO: find efficient zero-fill left:
                    int i = 0;
                    while (i < from) {
                        out[off + i] = 0;
                        i++;
                    }

                    int curAlpha = 0;
                    
                    while (i < ato) {
                        curAlpha += alphaDeltas[i];
//                    alphaDeltas[i] = 0;

                        // TODO: use Unsafe like MarlinCache
                        out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]                            
//                        out[off + i] = map[curAlpha];
                        i++;
                    }
//                alphaDeltas[i] = 0;

// TODO: find efficient zero-fill right:
                    while (i < w) {
                        out[off + i] = 0;
                        i++;
                    }
                }

                // Clear alpha row for reuse:
                IntArrayCache.fill(alphaDeltas, from, to + 1, 0);                
        
            } else {
                // Clear complete row:
               Arrays.fill(out, off, off + w, (byte)0);
            }
        }
    }
}
