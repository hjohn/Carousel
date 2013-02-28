package hs.javafx.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public class RayCarouselSkin<T> extends AbstractCarouselSkin<T> {

  public RayCarouselSkin(final TreeView<T> carousel) {
    super(carousel);

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        getSkinnable().requestLayout();
      }
    };

    cellAlignmentProperty().addListener(invalidationListener);
    reflectionEnabledProperty().addListener(invalidationListener);
    clipReflectionsProperty().addListener(invalidationListener);
    radiusRatioProperty().addListener(invalidationListener);
    viewDistanceRatioProperty().addListener(invalidationListener);
    maxCellWidthProperty().addListener(invalidationListener);
    maxCellHeightProperty().addListener(invalidationListener);
    viewAlignmentProperty().addListener(invalidationListener);
    carouselViewFractionProperty().addListener(invalidationListener);
  }

  @Override
  public Shape applyEffectsToCellAndReturnClip(TreeCell<T> cell, double index) {

    /*
     * Calculate the cells bounds adjusting for cell height, cell alignment and carousel
     * alignment in such a way that coordinate (0,0) will be where the carousel ring
     * intersects the cell.
     */

    Rectangle2D cellRectangle = calculateCellBounds(cell);

    if(getReflectionEnabled()) {

      /*
       * Do additional adjustments for the reflection.
       */

      cellRectangle = adjustCellRectangle(cellRectangle);
    }

    /*
     * Calculate where the cell bounds are in 3D space based on its index position on the
     * carousel.
     */

    Point3D[] points = calculateCarouselCoordinates(cellRectangle, index);

    /*
     * Apply additional transformations to the cell's 3D coordinates based on its index.
     */

    applyViewRotation(points, index);

    /*
     * Project the final position to 2D space.
     */

    Point2D[] projectedPoints = project(points);

    /*
     * Create the PerspectiveTransform and set it on the cell.
     */

    Point2D ul2d = projectedPoints[0];
    Point2D ur2d = projectedPoints[1];
    Point2D ll2d = projectedPoints[2];
    Point2D lr2d = projectedPoints[3];

    PerspectiveTransform perspectiveTransform = new PerspectiveTransform(ul2d.getX(), ul2d.getY(), ur2d.getX(), ur2d.getY(), lr2d.getX(), lr2d.getY(), ll2d.getX(), ll2d.getY());

    cell.setEffect(perspectiveTransform);

    /*
     * Add the reflection (if enabled) and return a clip for translucent areas (if enabled).
     */

    if(getReflectionEnabled()) {
      return adjustTransform(cell, perspectiveTransform);
    }

    return null;
  }

  protected Point2D[] project(Point3D[] points) {
    double viewDistance = getViewDistanceRatio() * getCarouselRadius();
    double fov = (viewDistance - getCarouselRadius());

    // Z = -1 when normalized

    Point2D[] projectedPoints = new Point2D[points.length];

    for(int i = 0; i < points.length; i++) {
      projectedPoints[i] = project(points[i], viewDistance, fov);
    }

    return projectedPoints;
  }

  protected Rectangle2D calculateCellBounds(TreeCell<T> cell) {
    Dimension2D cellSize = getNormalizedCellSize(cell);

    double halfCellWidth = 0.5 * cellSize.getWidth();
    double cellHeight = cellSize.getHeight();
    double maxCellHeight = getMaxCellHeight();

    return new Rectangle2D(
      -halfCellWidth,
      -maxCellHeight * (1.0 - getViewAlignment()) + (maxCellHeight - cellHeight) * getCellAlignment(),
      2 * halfCellWidth,
      cellHeight
    );
  }

  protected Rectangle2D adjustCellRectangle(Rectangle2D cellRectangle) {
    double reflectionMaxHeight = 50;

    double h = cellRectangle.getHeight();
    double unusedHeight = getMaxCellHeight() - h;

    double horizonDistance = unusedHeight - unusedHeight * getCellAlignment();
    double reflectionPortion = (reflectionMaxHeight - horizonDistance) / h;

    if(reflectionPortion < 0 || horizonDistance >= reflectionMaxHeight) {
      return cellRectangle;
    }

    if(reflectionPortion > 1) {
      reflectionPortion = 1;
    }

    return new Rectangle2D(
      cellRectangle.getMinX(),
      cellRectangle.getMinY(),
      cellRectangle.getWidth(),
      cellRectangle.getHeight() + horizonDistance * 2 + h * reflectionPortion
    );
  }

  private Point2D project(Point3D p, double viewDistance, double fov) {
    return new Point2D(snapPosition(p.getX() * fov / (p.getZ() + viewDistance)), snapPosition(p.getY() * fov / (p.getZ() + viewDistance)));
  }

  private static Point3D rotateY(Point3D p, Point3D axis, double radians) {
    Point3D input = new Point3D(p.getX() - axis.getX(), p.getY() - axis.getY(), p.getZ() - axis.getZ());

    return new Point3D(
      input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + axis.getX(),
      input.getY() + axis.getY(),
      input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + axis.getZ()
    );
  }

  private static final double REFLECTION_OPACITY = 0.5;

  protected Shape adjustTransform(TreeCell<T> cell, PerspectiveTransform perspectiveTransform) {
    double reflectionMaxHeight = 50;

    double cellHeight = getNormalizedCellSize(cell).getHeight();
    double unusedHeight = getMaxCellHeight() - cellHeight;

    double horizonDistance = unusedHeight - unusedHeight * getCellAlignment();
    double reflectionPortion = (reflectionMaxHeight - horizonDistance) / cellHeight;

    if(reflectionPortion < 0 || horizonDistance >= reflectionMaxHeight) {
      return null;
    }

    double reflectionTopOpacity = REFLECTION_OPACITY - REFLECTION_OPACITY / reflectionMaxHeight * horizonDistance;
    double reflectionBottomOpacity = 0;

    if(reflectionPortion > 1) {
      reflectionBottomOpacity = REFLECTION_OPACITY - REFLECTION_OPACITY / reflectionPortion;
      reflectionPortion = 1;
    }

    if(reflectionPortion > 0) {
      perspectiveTransform.setInput(new Reflection(2 * horizonDistance / cellHeight * cell.prefHeight(-1), reflectionPortion, reflectionTopOpacity, reflectionBottomOpacity));

      if(!getClipReflections()) {
        return null;
      }

      double reflectionY = cellHeight + horizonDistance * 2;
      double fullHeight = reflectionY + cellHeight * reflectionPortion;

      double reflectionLY = perspectiveTransform.getUly() + (perspectiveTransform.getLly() - perspectiveTransform.getUly()) / fullHeight * reflectionY;
      double reflectionRY = perspectiveTransform.getUry() + (perspectiveTransform.getLry() - perspectiveTransform.getUry()) / fullHeight * reflectionY;

      return new Polygon(
        perspectiveTransform.getUlx(), reflectionLY,
        perspectiveTransform.getUrx(), reflectionRY,
        perspectiveTransform.getLrx(), perspectiveTransform.getLry(),
        perspectiveTransform.getLlx(), perspectiveTransform.getLly()
      );
    }

    return null;
  }

  /**
   * Rotates the Cell towards the Viewer when it is close to the center.  Also mirrors
   * the cells after they passed the center to keep the Cells correctly visible for the
   * viewer.
   */
  @SuppressWarnings("static-method")
  protected void applyViewRotation(Point3D[] points, double index) {
    double cellsToRotate = 2;

    if(index < cellsToRotate) {
      double angle = index > -cellsToRotate ? Math.PI / 2 * -index / cellsToRotate + Math.PI / 2 : Math.PI;

      Point3D axis = new Point3D((points[0].getX() + points[1].getX()) / 2, 0, (points[0].getZ() + points[1].getZ()) / 2);

      for(int i = 0; i < points.length; i++) {
        points[i] = rotateY(points[i], axis, angle);
      }
    }
  }

  protected Point3D[] calculateCarouselCoordinates(Rectangle2D cellRectangle, double index) {
    double angleOnCarousel = 2 * Math.PI * getCarouselViewFraction() / getInternalVisibleCellsCount() * index + Math.PI * 0.5;

    double carouselRadius = getCarouselRadius();

    double cos = Math.cos(angleOnCarousel);
    double sin = -Math.sin(angleOnCarousel);

    double l = carouselRadius - cellRectangle.getMinX();
    double r = l - cellRectangle.getWidth();

    double lx = l * cos;
    double rx = r * cos;
    double ty = cellRectangle.getMinY();
    double by = ty + cellRectangle.getHeight();
    double lz = l * sin;
    double rz = r * sin;

    return new Point3D[] {new Point3D(lx, ty, lz), new Point3D(rx, ty, rz), new Point3D(lx, by, lz), new Point3D(rx, by, rz)};
  }

  protected double getCarouselRadius() {
    return getSkinnable().getWidth() * getRadiusRatio();
  }
}
