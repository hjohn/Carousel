package hs.javafx.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public class LinearCarouselSkin<T> extends AbstractCarouselSkin<T> {

  public LinearCarouselSkin(final TreeView<T> carousel) {
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
    maxCellWidthProperty().addListener(invalidationListener);
    maxCellHeightProperty().addListener(invalidationListener);
  }

  @Override
  public Shape applyEffectsToCellAndReturnClip(TreeCell<T> cell, double index) {

    /*
     * Calculate the cells bounds adjusting for cell height, cell alignment and carousel
     * alignment in such a way that coordinate (0,0) is the baseline of the cell.
     */

    Rectangle2D cellRectangle = calculateCellBounds(cell, getViewAlignment());

    if(getReflectionEnabled()) {

      /*
       * Do additional adjustments for the reflection.
       */

      cellRectangle = adjustCellRectangle(cellRectangle);
    }

    /*
     * Create the PerspectiveTransform and set it on the cell.
     */

    PerspectiveTransform perspectiveTransform = createPerspectiveTransform(cellRectangle, index);

    cell.setEffect(perspectiveTransform);

    /*
     * Add the reflection (if enabled) and return a clip for translucent areas (if enabled).
     */

    if(getReflectionEnabled()) {
      return adjustTransform(cell, perspectiveTransform);
    }

    return null;
  }

  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double index) {
    double offset = getSkinnable().getWidth() / getInternalVisibleCellsCount() * index;

    return new PerspectiveTransform(
      cellRectangle.getMinX() - offset, cellRectangle.getMinY(),
      cellRectangle.getMaxX() - offset, cellRectangle.getMinY(),
      cellRectangle.getMaxX() - offset, cellRectangle.getMaxY(),
      cellRectangle.getMinX() - offset, cellRectangle.getMaxY()
    );
  }

  @SuppressWarnings("static-method")
  protected double getViewAlignment() {
    return 0.5;
  }

  @Override
  protected double calculateCellCount() {
    int cellCount = (int)super.calculateCellCount();

    if(cellCount % 2 == 0) {
      cellCount--;
    }

    return cellCount;
  }

  protected Rectangle2D calculateCellBounds(TreeCell<T> cell, double viewAlignment) {
    Dimension2D cellSize = getNormalizedCellSize(cell);

    double halfCellWidth = 0.5 * cellSize.getWidth();
    double cellHeight = cellSize.getHeight();
    double maxCellHeight = getMaxCellHeight();

    return new Rectangle2D(
      -halfCellWidth,
      -maxCellHeight * viewAlignment + (maxCellHeight - cellHeight) * getCellAlignment(),
      2 * halfCellWidth,
      cellHeight
    );
  }

  protected Rectangle2D adjustCellRectangle(Rectangle2D cellRectangle) {
    double reflectionMaxHeight = 50;

    double height = cellRectangle.getHeight();
    double unusedHeight = getMaxCellHeight() - height;

    double horizonDistance = unusedHeight - unusedHeight * getCellAlignment();
    double reflectionPortion = (reflectionMaxHeight - horizonDistance) / height;

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
      cellRectangle.getHeight() + 2 * horizonDistance + height * reflectionPortion
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

      double reflectionY = cellHeight + 2 * horizonDistance;
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
}
