package hs.javafx.control;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;

public class RayCellIterator extends AbstractHorizontalCellIterator {
  private final RayLayout layout;
  private final int baseIndex;
  private final int minimumIndex;
  private final int maximumIndex;
  private final int cellCount;

  private Point3D[] points;
  private double currentCellDistanceToCenter;

  private int nextCount;
  private int previousCount;

  public RayCellIterator(RayLayout layout, double fractionalIndex) {
    super(layout.getSkin(), fractionalIndex);

    this.layout = layout;

    int centerIndex = getSkin().getSkinnable().getFocusModel().getFocusedIndex() - (int)Math.round(fractionalIndex);

    this.baseIndex = centerIndex == -1 ? 0 : centerIndex;

    int count = (int)calculateCellCount();

    this.cellCount = count % 2 == 0 ? count - 1 : count;  // always uneven
    this.minimumIndex = Math.max(0, centerIndex - cellCount / 2);
    this.maximumIndex = Math.min(getSkin().getSkinnable().getExpandedItemCount() - 1, centerIndex + cellCount / 2);
  }

  public int getCellCount() {
    return cellCount;
  }

  public Point3D[] currentPoints() {
    return points;
  }

  public double currentCellDistanceToCenter() {
    return currentCellDistanceToCenter;
  }

  @Override
  protected double calculateCellOffset(Rectangle2D cellRectangle) {
    return 0;
  }

  @Override
  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double offset) {
    this.currentCellDistanceToCenter = getSkin().getSkinnable().getFocusModel().getFocusedIndex() - current().getIndex() - getFractionalIndex();

    /*
     * Calculate where the cell bounds are in 3D space based on its index position on the
     * carousel.
     */

    this.points = calculateCarouselCoordinates(cellRectangle, currentCellDistanceToCenter);

    /*
     * Apply additional transformations to the cell's 3D coordinates based on its index.
     */

    layout.customizeCell(this);

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
      projectedPoints[2].getX(), projectedPoints[2].getY(),
      projectedPoints[3].getX(), projectedPoints[3].getY()
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

    return new Point3D[] {new Point3D(lx, ty, lz), new Point3D(rx, ty, rz), new Point3D(rx, by, rz), new Point3D(lx, by, lz)};
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
