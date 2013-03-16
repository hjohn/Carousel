package hs.javafx.control;

import java.util.NoSuchElementException;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.TreeCell;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.shape.Shape;

public abstract class AbstractCellIterator implements CellIterator {
  private final CarouselSkin<?> skin;
  private final double fractionalIndex;

  private TreeCell<?> currentCell;
  private Shape currentClip;

  public AbstractCellIterator(CarouselSkin<?> skin, double fractionalIndex) {
    this.skin = skin;
    this.fractionalIndex = fractionalIndex;
  }

  protected abstract int nextIndex();
  protected abstract double calculateCellOffset(Rectangle2D cellRectangle);
  protected abstract PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double offset);
  protected abstract Rectangle2D calculateCellBounds();
  protected abstract Rectangle2D adjustCellRectangle(Rectangle2D cellRectangle);
  protected abstract Shape adjustTransform(PerspectiveTransform perspectiveTransform);

  protected CarouselSkin<?> getSkin() {
    return skin;
  }

  protected double getFractionalIndex() {
    return fractionalIndex;
  }

  public TreeCell<?> current() {
    return currentCell;
  }

  @Override
  public Shape getClip() {
    return currentClip;
  }

  @Override
  public TreeCell<?> next() {
    if(!hasNext()) {
      throw new NoSuchElementException();
    }

    currentCell = skin.getCellPool().getCell(nextIndex());

    skin.getChildren().add(currentCell);  // Add to children so layout calculations are accurate

    /*
     * Calculate the cells bounds adjusting for cell height, cell alignment and carousel
     * alignment in such a way that coordinate (0,0) is the baseline of the cell.
     */

    Rectangle2D cellRectangle = calculateCellBounds();

    if(skin.getReflectionEnabled()) {

      /*
       * Do additional adjustments for the reflection.
       */

      cellRectangle = adjustCellRectangle(cellRectangle);
    }

    /*
     * Create the PerspectiveTransform and set it on the cell.
     */

    double offset = calculateCellOffset(cellRectangle);
    PerspectiveTransform perspectiveTransform = createPerspectiveTransform(cellRectangle, offset);

    currentCell.setEffect(perspectiveTransform);

    /*
     * Add the reflection (if enabled) and return a clip for translucent areas (if enabled).
     */

    this.currentClip = null;

    if(skin.getReflectionEnabled()) {
      this.currentClip = adjustTransform(perspectiveTransform);
    }

    return currentCell;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
