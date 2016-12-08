FILES="CollinearSimplifier Curve Dasher Renderer RendererNoAA Stroker TransformingPathConsumer2D"

for f in $FILES
do
  echo "Processing $f"
  CL="s/$f/D$f/g"
  sed -e $CL -e 's/import com.sun.javafx.geom.PathConsumer2D;//g' -e 's/PathConsumer2D/DPathConsumer2D/g' -e 's/DTransformingDPathConsumer2D/DTransformingPathConsumer2D/g' -e 's/(float)//g' -e 's/float/double/g' -e 's/Float/Double/g' -e 's/DoubleMath/FloatMath/g' -e 's/\([0-9]*\.\?[0-9]\+\)f/\1D/g' -e 's/Curve/DCurve/g' -e 's/DDCurve/DCurve/g' -e 's/Helpers/DHelpers/g' -e 's/DDHelpers/DHelpers/g' -e 's/MarlinRenderer/DMarlinRenderer/g' -e 's/RendererContext/DRendererContext/g' -e 's/DDRendererContext/DRendererContext/g' -e 's/MarlinDRenderer/DMarlinRenderer/g' < $f.java > D$f.java
done

# Only discard within(float) in Helper
  echo "Processing Helpers"
  sed -e 's/import com.sun.javafx.geom.PathConsumer2D;//g' -e 's/PathConsumer2D/DPathConsumer2D/g' -e 's/DTransformingDPathConsumer2D/DTransformingPathConsumer2D/g' -e 's/static boolean within(final float x/static boolean withinUNUSED(final float x/g' -e 's/(float)//g' -e 's/float/double/g' -e 's/Float/Double/g' -e 's/DoubleMath/FloatMath/g' -e 's/\([0-9]*\.\?[0-9]\+\)f/\1D/g' -e 's/Curve/DCurve/g' -e 's/DDCurve/DCurve/g' -e 's/Helpers/DHelpers/g' -e 's/MarlinRenderer/DMarlinRenderer/g' -e 's/RendererContext/DRendererContext/g' -e 's/DDRendererContext/DRendererContext/g' -e 's/MarlinDRenderer/DMarlinRenderer/g' < Helpers.java > DHelpers.java

# \([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)f

