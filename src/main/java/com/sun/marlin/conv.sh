FILES="CollinearSimplifier Curve Renderer RendererNoAA Stroker TransformingPathConsumer2D"

for f in $FILES
do
  echo "Processing $f"
  sed -e "s/$f/D$f/g" -e "s/\"D$f/\"$f/g" -e 's/import com.sun.javafx.geom.PathConsumer2D;//g' -e 's/PathConsumer2D/DPathConsumer2D/g' -e 's/DTransformingDPathConsumer2D/DTransformingPathConsumer2D/g' -e 's/(float)//g' -e 's/float/double/g' -e 's/Float/Double/g' -e 's/DoubleMath/FloatMath/g' -e 's/\([0-9]*\.\?[0-9]\+\)f/\1d/g' -e 's/ Curve/ DCurve/g' -e 's/Helpers/DHelpers/g' -e 's/MarlinRenderer/DMarlinRenderer/g' -e 's/RendererContext/DRendererContext/g'  -e "s/DD$f/D$f/g" -e 's/MarlinDRenderer/DMarlinRenderer/g' -e 's/doubleing/floating/g' < $f.java > D$f.java
done

# Dasher (convert type)
sed -e 's/Dasher/DDasher/g' -e 's/import com.sun.javafx.geom.PathConsumer2D;//g' -e 's/PathConsumer2D/DPathConsumer2D/g' -e 's/DTransformingDPathConsumer2D/DTransformingPathConsumer2D/g' -e 's/(float)//g' -e 's/float/double/g' -e 's/Float/Double/g' -e 's/DoubleMath/FloatMath/g' -e 's/\([0-9]*\.\?[0-9]\+\)f/\1d/g' -e 's/ Curve/ DCurve/g' -e 's/Helpers/DHelpers/g' -e 's/MarlinRenderer/DMarlinRenderer/g' -e 's/RendererContext/DRendererContext/g' -e 's/MarlinDRenderer/DMarlinRenderer/g' -e 's/copyDashArray(final double\[\] dashes)/copyDashArray(final float\[\] dashes)/g' -e 's/System.arraycopy(dashes, 0, newDashes, 0, len);/for \(int i = 0; i < len; i\+\+\) \{ newDashes\[i\] = dashes\[i\]; \}/g'< Dasher.java > DDasher.java


# Only discard within(float) in Helper
  echo "Processing Helpers"
  sed -e 's/import com.sun.javafx.geom.PathConsumer2D;//g' -e 's/PathConsumer2D/DPathConsumer2D/g' -e 's/DTransformingDPathConsumer2D/DTransformingPathConsumer2D/g' -e 's/static boolean within(final float x/static boolean withinUNUSED(final float x/g' -e 's/(float)//g' -e 's/float/double/g' -e 's/Float/Double/g' -e 's/DoubleMath/FloatMath/g' -e 's/\([0-9]*\.\?[0-9]\+\)f/\1d/g' -e 's/ Curve/ DCurve/g' -e 's/Helpers/DHelpers/g' -e 's/MarlinRenderer/DMarlinRenderer/g' -e 's/RendererContext/DRendererContext/g' -e 's/MarlinDRenderer/DMarlinRenderer/g' < Helpers.java > DHelpers.java

