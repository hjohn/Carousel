package hs.javafx.carousel;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point3D;

public class RayLayout implements Layout {
  private final DoubleProperty radiusRatio = new SimpleDoubleProperty(1.0);
  public final DoubleProperty radiusRatioProperty() { return radiusRatio; }
  public final double getRadiusRatio() { return radiusRatio.get(); }

  private final DoubleProperty viewDistanceRatio = new SimpleDoubleProperty(2.0);
  public final DoubleProperty viewDistanceRatioProperty() { return viewDistanceRatio; }
  public final double getViewDistanceRatio() { return viewDistanceRatio.get(); }

  private final DoubleProperty viewAlignment = new SimpleDoubleProperty(0.5);
  public final DoubleProperty viewAlignmentProperty() { return viewAlignment; }
  public final double getViewAlignment() { return viewAlignment.get(); }

  private final DoubleProperty carouselViewFraction = new SimpleDoubleProperty(0.5);
  public final DoubleProperty carouselViewFractionProperty() { return carouselViewFraction; }
  public final double getCarouselViewFraction() { return carouselViewFraction.get(); }

  private final CarouselSkin<?> skin;

  public RayLayout(final CarouselSkin<?> skin) {
    this.skin = skin;

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        skin.getSkinnable().requestLayout();
      }
    };

    radiusRatioProperty().addListener(invalidationListener);
    viewDistanceRatioProperty().addListener(invalidationListener);
    viewAlignmentProperty().addListener(invalidationListener);
    carouselViewFractionProperty().addListener(invalidationListener);
  }

  @Override
  public CellIterator renderCellIterator(double fractionalIndex) {
    return new RayCellIterator(this, fractionalIndex);
  }

  @Override
  public CarouselSkin<?> getSkin() {
    return skin;
  }

  /**
   * Called by the CellIterator for each cell to allow broad customization of the
   * current cell.  The customization options are CellIterator specific.  To customize
   * a cell, examine the CellIterator's state and apply adjustments to the cell itself
   * or any of the intermediate values being calculated (if provided).<p>
   *
   * @param iterator a CellIterator
   */
  protected void customizeCell(RayCellIterator iterator) {
    rotateCenterCellsTowardsViewer(iterator, 2.0);
    fadeOutEdgeCells(iterator, 0.5);
  }

  @SuppressWarnings("static-method")
  protected void fadeOutEdgeCells(RayCellIterator iterator, double fadeOutCellCount) {
    double index = Math.abs(iterator.currentCellDistanceToCenter());
    double fadeOutDistance = iterator.getCellCount() / 2 - fadeOutCellCount + 0.5;

    if(index > fadeOutDistance) {
      iterator.current().setOpacity(1.0 - (index - fadeOutDistance) / fadeOutCellCount);
    }
    else {
      iterator.current().setOpacity(1.0);
    }
  }

  @SuppressWarnings("static-method")
  protected void rotateCenterCellsTowardsViewer(RayCellIterator iterator, double cellsToRotate) {
    Point3D[] points = iterator.currentPoints();
    double index = iterator.currentCellDistanceToCenter();

    if(index < cellsToRotate) {
      double angle = index > -cellsToRotate ? 0.5 * Math.PI * -index / cellsToRotate + 0.5 * Math.PI : Math.PI;

      Point3D center = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);

      for(int i = 0; i < points.length; i++) {
        points[i] = rotateY(points[i], center, angle);
      }
    }
  }

  protected static Point3D rotateY(Point3D p, Point3D center, double radians) {
    Point3D input = new Point3D(p.getX() - center.getX(), p.getY() - center.getY(), p.getZ() - center.getZ());

    return new Point3D(
      input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + center.getX(),
      input.getY() + center.getY(),
      input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + center.getZ()
    );
  }
}
