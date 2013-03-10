package hs.javafx.control;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.TreeView;
import javafx.scene.effect.PerspectiveTransform;

public class LinearCellIterator<T> extends AbstractHorizontalCellIterator<T> {
  private final TreeView<T> treeView;

  private int nextCount;
  private int previousCount;
  private int baseIndex;

  private final boolean spaced;

  private final double halfWidth;
  private double minX = Double.MAX_VALUE;
  private double maxX = Double.MIN_VALUE;

  private final int minimumIndex;
  private final int maximumIndex;
  private final int cellCount;

  public LinearCellIterator(LinearLayout<T> layout, double fractionalIndex, boolean spaced) {
    super(layout.getSkin(), fractionalIndex);

    this.spaced = spaced;
    this.treeView = getSkin().getSkinnable();

    int centerIndex = treeView.getFocusModel().getFocusedIndex() - (int)Math.round(fractionalIndex);
    this.baseIndex = centerIndex == -1 ? 0 : centerIndex;

    if(spaced) {
      int preferredCellCount = (int)calculateCellCount();

      if(preferredCellCount % 2 == 0) {
        preferredCellCount--;
      }

      this.cellCount = preferredCellCount;
      this.minimumIndex = Math.max(0, centerIndex - (cellCount - 1) / 2);
      this.maximumIndex = Math.min(treeView.getExpandedItemCount() - 1, centerIndex + cellCount / 2);
      this.halfWidth = Double.MAX_VALUE;
    }
    else {
      this.cellCount = 0;
      this.minimumIndex = 0;
      this.maximumIndex = treeView.getExpandedItemCount() - 1;
      this.halfWidth = treeView.getWidth() / 2;
    }
  }

  protected double calculateCellCount() {
    double count = treeView.getWidth() * getSkin().getDensity();

    return count < 3 ? 3 : count;
  }

  private boolean hasMoreLeftCells() {
    return minX > -halfWidth && baseIndex - previousCount - 1 >= minimumIndex;
  }

  private boolean hasMoreRightCells() {
    return maxX < halfWidth && baseIndex + nextCount <= maximumIndex;
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

  @Override
  protected double getViewAlignment() {
    return 0.5;
  }

  // TODO figure out how to make non-spaced and spaced versions of LinearCarouselSkin switchable by boolean
  // TODO need fade out in spaced version
  // TODO cut-off by X coordinate for space version doesn't work, must be by cell count
  // TODO cell density is meaningless for non-spaced version; cell spacing is only meaningful for non-spaced version

  // TODO Only cell rectangle gets manipulated by offset, why not manipulate cell rectangle directly then?
  // TODO Check calculate cell count...
  // TODO FractionalIndex... provide with CellRuns or make available on Skin?

  @Override
  protected double calculateCellOffset(Rectangle2D cellRectangle) {
    double index = treeView.getFocusModel().getFocusedIndex() - current().getIndex() - getFractionalIndex();
    double spacing = 10;
    double offset;

    if(spaced) {
      offset = getSkin().getSkinnable().getWidth() / cellCount * index;
    }
    else {
      if(minX == Double.MAX_VALUE && maxX == Double.MIN_VALUE) {
        offset = cellRectangle.getWidth() * index;  // TODO this isn't perfect, scroll speed will depend on width of the center cell
      }
      else {
        if(index < 0) {
          offset = -(maxX + cellRectangle.getWidth() / 2 + spacing);
        }
        else {
          offset = -(minX - cellRectangle.getWidth() / 2 - spacing);
        }
      }
    }

    return offset;
  }

  @Override
  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double offset) {
    PerspectiveTransform perspectiveTransform = new PerspectiveTransform(
      cellRectangle.getMinX() - offset, cellRectangle.getMinY(),
      cellRectangle.getMaxX() - offset, cellRectangle.getMinY(),
      cellRectangle.getMaxX() - offset, cellRectangle.getMaxY(),
      cellRectangle.getMinX() - offset, cellRectangle.getMaxY()
    );

    minX = Math.min(minX, perspectiveTransform.getUlx());
    maxX = Math.max(maxX, perspectiveTransform.getUrx());

    return perspectiveTransform;
  }
}
