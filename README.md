MarlinFX renderer
=================

MarlinFX is the JavaFX port of the [Marlin-renderer](https://github.com/bourgesl/marlin-renderer) (scanline rasterizer only) aimed to be faster than Open/Native Pisces.

License
=======

As marlin is a fork from OpenJDK Pisces, its license is the OpenJDK's license = GPL2+CP:

GNU General Public License, version 2,
with the Classpath Exception

The GNU General Public License (GPL)

Version 2, June 1991

See License.md

Build
=====

Needs Maven + Oracle or Open JDK 1.8 (with JavaFX)

The MarlinFX build produces a (big) JavaFX jar file patched with MarlinFX classes (com.sun.marlin + hacked ShapeUtil using the MarlinRasterizer) to be placed in the boot classpath as JavaFX 8 lies in the extension classpath (and can not be patched easily). Of course, such (complete) JavaFX jar depends on your JDK version and your platform (win, mac, linux ...) so the MarlinFX jar can not be distributed (license issue) nor shared across platforms (binary incompatiblity).

Note: it does not modify the SW pipeline which still uses OpenPisces (for compatiblity issue).

First time, import your local jfxrt.jar (from ``${java.home}/lib/ext/jfxrt.jar``) to your local maven repository or when you get the error 'Could not find artifact javafx:jfxrt:jar:local in central (https://repo.maven.apache.org/maven2)' :

``mvn process-resources``

Then build MarlinFX:

``mvn clean install``

or

``mvn -Dmaven.test.skip=true clean install`` (to skip running tests)

The MarlinFX jar is available in the target folder like:

``target/marlinfx-0.7.5-Unsafe.jar``


Usage
=====

For testing purposes (only), MarlinFX can be used with any JavaFX application running on Oracle or Open JDK 8 (and derived JVMs).

Just put the marlinfx-x.y.jar file in the bootclasspath to let JavaFX use MarlinFX instead of OpenPisces (java rasterizer) and set the following system property prism.marlin=true/false (true by default):

``java -Xbootclasspath/p:[absolute or relative path]/marlinfx-0.7.5-Unsafe.jar -Dprism.marlin=true ...``

For example to launch the JavaFX8 Ensemble demo:

``java -Xbootclasspath/p:/home/bourgesl/libs/marlin/branches/marlin-fx/target/marlinfx-0.7.5-Unsafe.jar -jar Ensemble8.jar``

You should see MarlinFX in action and the following message will be present in the console:
```
Marlin-FX[marlinFX-0.7.5-Unsafe-OpenJDK] (double) enabled.
INFO: ===============================================================================
INFO: Marlin software rasterizer    = ENABLED
INFO: Version                       = [marlinFX-0.7.5-Unsafe-OpenJDK]
INFO: prism.marlin                  = com.sun.marlin.DRenderer
INFO: prism.marlin.useThreadLocal   = true
INFO: prism.marlin.useRef           = soft
INFO: prism.marlin.edges            = 4096
INFO: prism.marlin.pixelsize        = 2048
INFO: prism.marlin.subPixel_log2_X  = 3
INFO: prism.marlin.subPixel_log2_Y  = 3
INFO: prism.marlin.blockSize_log2   = 5
INFO: prism.marlin.forceRLE         = false
INFO: prism.marlin.forceNoRLE       = false
INFO: prism.marlin.useTileFlags     = true
INFO: prism.marlin.useTileFlags.useHeuristics = true
INFO: prism.marlin.rleMinWidth      = 64
INFO: prism.marlin.useSimplifier    = false
INFO: prism.marlin.doStats          = false
INFO: prism.marlin.doMonitors       = false
INFO: prism.marlin.doChecks         = true
INFO: prism.marlin.log              = true
INFO: prism.marlin.useLogger        = true
INFO: prism.marlin.logCreateContext = false
INFO: prism.marlin.logUnsafeMalloc  = false
INFO: Renderer settings:
INFO: CUB_COUNT_LG = 2
INFO: CUB_DEC_BND  = 8.0
INFO: CUB_INC_BND  = 3.2
INFO: QUAD_DEC_BND = 4.0
INFO: INITIAL_EDGES_CAPACITY        = 98304
INFO: INITIAL_CROSSING_COUNT        = 1024
INFO: ===============================================================================
```
Two pipelines are available based on Double (default) and Float numerical values. To select the MarlinFX pipeline, use the system property prism.marlin.double=true/false (true means Double, false means Float) and is indicated in the standard output.


Enjoy and send us your feedback !

Note: Marlin system properties have been renamed to use the prefix 'prism.marlin' like prism.marlin.log=true/false (true by default).


Getting in touch
================

Users and developers interested in the Marlin-renderer are kindly invited to join the [marlin-renderer](https://groups.google.com/forum/#!forum/marlin-renderer) Google Group.


Related projects
===============

[Marlin-renderer](https://github.com/bourgesl/marlin-renderer) is the main Marlin-renderer repository (java2D rendering engine).
[Mapbench](https://github.com/bourgesl/mapbench) provides benchmarking tools based on real world map painted by the [GeoServer](http://geoserver.org/) WMS server
