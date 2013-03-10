package hs.javafx.control;

import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public abstract class AbstractHorizontalCellIterator<T> extends AbstractCellIterator<T> {

  public AbstractHorizontalCellIterator(CarouselSkin<T> skin, double fractionalIndex) {
    super(skin, fractionalIndex);
  }

  protected abstract double getViewAlignment();

  /**
   * Returns the width and height of the cell when it is made to fit within
   * the MaxCellWidth and MaxCellHeight restrictions while preserving the aspect
   * ratio.
   *
   * @param cell a cell to calculate the dimensions for
   * @return the normalized dimensions
   */
  protected Dimension2D getNormalizedCellSize() {
    double prefWidth = current().prefWidth(-1);
    double prefHeight = current().prefHeight(-1);

    if(prefWidth > getSkin().getMaxCellWidth()) {
      prefHeight = prefHeight / prefWidth * getSkin().getMaxCellWidth();
      prefWidth = getSkin().getMaxCellWidth();
    }
    if(prefHeight > getSkin().getMaxCellHeight()) {
      prefWidth = prefWidth / prefHeight * getSkin().getMaxCellHeight();
      prefHeight = getSkin().getMaxCellHeight();
    }

    return new Dimension2D(prefWidth, prefHeight);
  }

  @Override
  protected Rectangle2D calculateCellBounds() {
    Dimension2D cellSize = getNormalizedCellSize();

    double halfCellWidth = 0.5 * cellSize.getWidth();
    double cellHeight = cellSize.getHeight();
    double maxCellHeight = getSkin().getMaxCellHeight();

    return new Rectangle2D(
      -halfCellWidth,
      -maxCellHeight * getViewAlignment() + (maxCellHeight - cellHeight) * getSkin().getCellAlignment(),
      2 * halfCellWidth,
      cellHeight
    );
  }

  @Override
  protected Rectangle2D adjustCellRectangle(Rectangle2D cellRectangle) {
    double reflectionMaxHeight = 50;

    double height = cellRectangle.getHeight();
    double unusedHeight = getSkin().getMaxCellHeight() - height;

    double horizonDistance = unusedHeight - unusedHeight * getSkin().getCellAlignment();
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

  @Override
  protected Shape adjustTransform(PerspectiveTransform perspectiveTransform) {
    double reflectionMaxHeight = 50;

    double cellHeight = getNormalizedCellSize().getHeight();
    double unusedHeight = getSkin().getMaxCellHeight() - cellHeight;

    double horizonDistance = unusedHeight - unusedHeight * getSkin().getCellAlignment();
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
      perspectiveTransform.setInput(new Reflection(2 * horizonDistance / cellHeight * current().prefHeight(-1), reflectionPortion, reflectionTopOpacity, reflectionBottomOpacity));

      if(!getSkin().getClipReflections()) {
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
