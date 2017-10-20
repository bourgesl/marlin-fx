/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * Type needed to be handled specifically by the Marlin renderer
 * @author bourgesl
 */
public abstract class NGCanvasPath extends Shape {

    public abstract Path2D getGeometry();

    public abstract BaseTransform getCombinedTransform(BaseTransform tx);

}
