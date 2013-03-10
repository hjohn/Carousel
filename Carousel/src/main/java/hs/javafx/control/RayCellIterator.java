package hs.javafx.control;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;

public class RayCellIterator<T> extends AbstractHorizontalCellIterator<T> {
  private final RayLayout<T> layout;
  private final int baseIndex;
  private final int minimumIndex;
  private final int maximumIndex;
  private final double cellCount;

  private int nextCount;
  private int previousCount;

  public RayCellIterator(RayLayout<T> layout, double fractionalIndex) {
    super(layout.getSkin(), fractionalIndex);

    this.layout = layout;

    int centerIndex = getSkin().getSkinnable().getFocusModel().getFocusedIndex() - (int)Math.round(fractionalIndex);

    this.baseIndex = centerIndex == -1 ? 0 : centerIndex;
    this.cellCount = calculateCellCount();

    int preferredCellCount = (int)cellCount;

    if(preferredCellCount % 2 == 0) {
      preferredCellCount--;
    }

    this.minimumIndex = Math.max(0, centerIndex - (preferredCellCount - 1) / 2);
    this.maximumIndex = Math.min(getSkin().getSkinnable().getExpandedItemCount() - 1, centerIndex + preferredCellCount / 2);
  }

  @Override
  protected double calculateCellOffset(Rectangle2D cellRectangle) {
    return 0;
  }

  @Override
  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double offset) {
    double index = getSkin().getSkinnable().getFocusModel().getFocusedIndex() - current().getIndex() - getFractionalIndex();

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

    return new PerspectiveTransform(
      projectedPoints[0].getX(), projectedPoints[0].getY(),
      projectedPoints[1].getX(), projectedPoints[1].getY(),
      projectedPoints[3].getX(), projectedPoints[3].getY(),
      projectedPoints[2].getX(), projectedPoints[2].getY()
    );
  }

  @Override
  protected double getViewAlignment() {
    return layout.getViewAlignment();
  }

  protected Point2D[] project(Point3D[] points) {
    double carouselRadius = getCarouselRadius();
    double viewDistance = layout.getViewDistanceRatio() * carouselRadius;
    double fov = viewDistance - carouselRadius;
    double horizonY = getSkin().getMaxCellHeight() * getViewAlignment() - 0.5 * getSkin().getMaxCellHeight();

    Point2D[] projectedPoints = new Point2D[points.length];

    for(int i = 0; i < points.length; i++) {
      projectedPoints[i] = project(points[i], viewDistance, fov, horizonY);
    }

    return projectedPoints;
  }

  private static Point2D project(Point3D p, double viewDistance, double fov, double horizonY) {
    return new Point2D(p.getX() * fov / (p.getZ() + viewDistance), p.getY() * fov / (p.getZ() + viewDistance) + horizonY);
  }

  private static Point3D rotateY(Point3D p, Point3D axis, double radians) {
    Point3D input = new Point3D(p.getX() - axis.getX(), p.getY() - axis.getY(), p.getZ() - axis.getZ());

    return new Point3D(
      input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + axis.getX(),
      input.getY() + axis.getY(),
      input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + axis.getZ()
    );
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
      double angle = index > -cellsToRotate ? 0.5 * Math.PI * -index / cellsToRotate + 0.5 * Math.PI : Math.PI;

      Point3D axis = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);

      for(int i = 0; i < points.length; i++) {
        points[i] = rotateY(points[i], axis, angle);
      }
    }
  }

  protected Point3D[] calculateCarouselCoordinates(Rectangle2D cellRectangle, double index) {
    double angleOnCarousel = 2 * Math.PI * layout.getCarouselViewFraction() / calculateCellCount() * index + 0.5 * Math.PI;

    double cos = Math.cos(angleOnCarousel);
    double sin = -Math.sin(angleOnCarousel);

    double l = getCarouselRadius() - cellRectangle.getMinX();
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
    return getSkin().getSkinnable().getWidth() * layout.getRadiusRatio();
  }

  protected double calculateCellCount() {
    double count = getSkin().getSkinnable().getWidth() * getSkin().getDensity();

    return count < 3 ? 3 : count;
  }

  private boolean hasMoreLeftCells() {
    return baseIndex - previousCount - 1 >= minimumIndex;
  }

  private boolean hasMoreRightCells() {
    return baseIndex + nextCount <= maximumIndex;
  }

  @Override
  public boolean hasNext() {
    return hasMoreLeftCells() || hasMoreRightCells();
  }

  @Override
  protected int nextIndex() {
    if((hasMoreLeftCells() && previousCount < nextCount) || !hasMoreRightCells()) {
      return baseIndex - previousCount++ - 1;
    }

    return baseIndex + nextCount++;
  }
}
