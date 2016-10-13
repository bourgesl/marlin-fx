MarlinFX renderer
=================

MarlinFX is the JavaFX port of the Marlin renderer (scanline rasterizer only) aimed to be faster than Open/Native Pisces (notably for very complex paths)

License
=======

As marlin is a fork from OpenJDK 8 pisces, its license is the OpenJDK's license = GPL2+CP:

GNU General Public License, version 2,
with the Classpath Exception

The GNU General Public License (GPL)

Version 2, June 1991

See License.md

Build
=====

Needs Maven + Oracle or Open JDK 1.8 (with JavaFX)

MarlinFX build produces a (big) JavaFX library patched with MarlinFX (com.sun.marlin + custom OpenPiscesRasterizer) to be placed in the boot classpath as JavaFX 8 lies in the extension classpath (and can not be patched easily). Of course, such (complete) JavaFX jar depends on your JDK version and your platform (win, mac, linux ...) so the MarlinFX jar can not be distributed (license issue) nor shared across platforms (incompatiblity).

First time, import your local jfxrt.jar (from ${java.home}/lib/ext/jfxrt.jar) to your local maven repository or when you get the error 'Could not find artifact javafx:jfxrt:jar:local in central (https://repo.maven.apache.org/maven2)' :
mvn process-resources

Then build MarlinFX:
mvn clean install

The MarlinFX jar is available in the target folder like:
target/marlinfx-0.7.5-Unsafe.jar


Usage
=====

For testing purposes (only ?), MarlinFX can be used with any JavaFX application running on Oracle or Open JDK 1.8.

Just put it in your bootclasspath to make JavaFX use MarlinFX instead of OpenPisces:

java -Xbootclasspath/p:[absolute or relative path]/marlinfx-0.7.5-Unsafe.jar -Dprism.nativepisces=false ...

Enjoy !


Getting in touch
================

Users and developers interested in the Marlin-renderer are kindly invited to join the [marlin-renderer](https://groups.google.com/forum/#!forum/marlin-renderer) Google Group.

Related projects
===============

[Marlin-renderer](https://github.com/bourgesl/marlin-renderer) is the main Marlin-renderer repository.
[Mapbench](https://github.com/bourgesl/mapbench) provides benchmarking tools based on real world map painted by the [GeoServer](http://geoserver.org/) WMS server
