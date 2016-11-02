MarlinFX renderer
=================

MarlinFX is the JavaFX port of the Marlin renderer (scanline rasterizer only) aimed to be faster than Open/Native Pisces (most effective with very complex paths or complex scenes).

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

First time, import your local jfxrt.jar (from ${java.home}/lib/ext/jfxrt.jar) to your local maven repository or when you get the error 'Could not find artifact javafx:jfxrt:jar:local in central (https://repo.maven.apache.org/maven2)' :
mvn process-resources

Then build MarlinFX:
mvn clean install

The MarlinFX jar is available in the target folder like:
target/marlinfx-0.7.5-Unsafe.jar


Usage
=====

For testing purposes (only), MarlinFX can be used with any JavaFX application running on Oracle or Open JDK 8 (and derived JVMs).

Just put the marlinfx-x.y.jar file in the bootclasspath to let JavaFX use MarlinFX instead of OpenPisces (java rasterizer) and set the following system properties sun.javafx.marlin=true and prism.nativepisces=false:

java -Xbootclasspath/p:[absolute or relative path]/marlinfx-0.7.5-Unsafe.jar -Dsun.javafx.marlin=true -Dprism.nativepisces=false ...

For example to launch the JavaFX8 Ensemble demo:
java -Xbootclasspath/p:/home/bourgesl/libs/marlin/branches/marlin-fx/target/marlinfx-0.7.5-Unsafe.jar -Dsun.javafx.marlin=true  -Dprism.nativepisces=false -jar Ensemble8.jar

You should see MarlinFX in action and the following message will be present in the console:

Marlin-FX[marlinFX-0.7.5-Unsafe-OpenJDK] enabled.

Enjoy and send us your feedback !


Getting in touch
================

Users and developers interested in the Marlin-renderer are kindly invited to join the [marlin-renderer](https://groups.google.com/forum/#!forum/marlin-renderer) Google Group.


Related projects
===============

[Marlin-renderer](https://github.com/bourgesl/marlin-renderer) is the main Marlin-renderer repository (java2D rendering engine).
[Mapbench](https://github.com/bourgesl/mapbench) provides benchmarking tools based on real world map painted by the [GeoServer](http://geoserver.org/) WMS server
