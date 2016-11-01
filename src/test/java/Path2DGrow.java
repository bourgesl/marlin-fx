/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Path2D;

/**
 * @test
 * @bug TODO
 * @summary Check the growth algorithm (needRoom) in Path2D implementations
 * @run main Path2DGrow
 */
public class Path2DGrow {

    public static final int N = 1000 * 1000;

/*
Before Patch:

 - Test(Path2D.Float[0]) ---
testAddMoves[1000000] duration= 9.520323 ms.
testAddLines[1000000] duration= 1360.9874399999999 ms.
testAddQuads[1000000] duration= 5116.442005 ms.
testAddCubics[1000000] duration= 11474.929682 ms.
testAddMoveAndCloses[1000000] duration= 1832.645204 ms.

 - Test(Path2D.Float) ---
testAddMoves[1000000] duration= 3.0425579999999997 ms.
testAddLines[1000000] duration= 1329.174734 ms.
testAddQuads[1000000] duration= 5106.395085 ms.
testAddCubics[1000000] duration= 11663.637824 ms.
testAddMoveAndCloses[1000000] duration= 1791.3164199999999 ms.

After patch:

 - Test(Path2D.Float[0]) ---
testAddMoves[1000000] duration= 6.104508999999999 ms.
testAddLines[1000000] duration= 24.262044 ms.
testAddQuads[1000000] duration= 48.584481 ms.
testAddCubics[1000000] duration= 67.556625 ms.
testAddMoveAndCloses[1000000] duration= 27.428037999999997 ms.

 - Test(Path2D.Float) ---
testAddMoves[1000000] duration= 4.898639 ms.
testAddLines[1000000] duration= 24.943267 ms.
testAddQuads[1000000] duration= 39.569337 ms.
testAddCubics[1000000] duration= 56.441154999999995 ms.
testAddMoveAndCloses[1000000] duration= 23.394513999999997 ms.
*/

    public static boolean verbose = true;
    public static boolean force = false;

    static void echo(String msg) {
        System.out.println(msg);
    }

    static void log(String msg) {
        if (verbose || force) {
            echo(msg);
        }
    }

    public static void main(String argv[]) {
        verbose = (argv.length != 0);

        testEmptyFloatPaths();
        testFloatPaths();
    }

    static void testEmptyFloatPaths() {
        echo("\n - Test(Path2D.Float[0]) ---");
        test(() -> new Path2D(Path2D.WIND_NON_ZERO, 0));
    }

    static void testFloatPaths() {
        echo("\n - Test(Path2D.Float) ---");
        test(() -> new Path2D());
    }

    interface PathFactory {
        Path2D makePath();
    }

    static void test(PathFactory pf) {
        long start, end;

        for (int n = 1; n <= N; n *= 10) {
            force = (n == N);

            start = System.nanoTime();
            testAddMoves(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddMoves[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddLines(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddLines[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddQuads(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddQuads[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddCubics(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddCubics[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddMoveAndCloses(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddMoveAndCloses[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");
        }
    }

    static void addMove(Path2D p2d, int i) {
        p2d.moveTo(1.0f * i, 0.5f * i);
    }

    static void addLine(Path2D p2d, int i) {
        p2d.lineTo(1.1f * i, 2.3f * i);
    }

    static void addCubic(Path2D p2d, int i) {
        p2d.curveTo(1.1f * i, 1.2f * i, 1.3f * i, 1.4f * i, 1.5f * i, 1.6f * i);
    }

    static void addQuad(Path2D p2d, int i) {
        p2d.quadTo(1.1f * i, 1.2f * i, 1.3f * i, 1.4f * i);
    }

    static void addClose(Path2D p2d) {
        p2d.closePath();
    }

    static void testAddMoves(Path2D pathA, int n) {
        for (int i = 0; i < n; i++) {
            addMove(pathA, i);
        }
    }

    static void testAddLines(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addLine(pathA, i);
        }
    }

    static void testAddQuads(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addQuad(pathA, i);
        }
    }

    static void testAddCubics(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addCubic(pathA, i);
        }
    }

    static void testAddMoveAndCloses(Path2D pathA, int n) {
        for (int i = 0; i < n; i++) {
            addMove(pathA, i);
            addClose(pathA);
        }
    }
}
