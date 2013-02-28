package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public abstract class AbstractCarouselSkin<T> extends AbstractTreeViewSkin<T> {
  private final DoubleProperty cellAlignment = new SimpleDoubleProperty(0.8);
  public final DoubleProperty cellAlignmentProperty() { return cellAlignment; }
  public final double getCellAlignment() { return cellAlignment.get(); }

  private final BooleanProperty reflectionEnabled = new SimpleBooleanProperty(true);
  public final BooleanProperty reflectionEnabledProperty() { return reflectionEnabled; }
  public final boolean getReflectionEnabled() { return reflectionEnabled.get(); }

  private final BooleanProperty clipReflections = new SimpleBooleanProperty(true);
  public final BooleanProperty clipReflectionsProperty() { return clipReflections; }
  public final boolean getClipReflections() { return clipReflections.get(); }

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

  private final DoubleProperty density = new SimpleDoubleProperty(0.02);
  public final DoubleProperty densityProperty() { return density; }
  public final double getDensity() { return density.get(); }

  private final DoubleProperty maxCellWidth = new SimpleDoubleProperty(300);
  public final DoubleProperty maxCellWidthProperty() { return maxCellWidth; }
  public final double getMaxCellWidth() { return maxCellWidth.get(); }

  private final DoubleProperty maxCellHeight = new SimpleDoubleProperty(200);
  public final DoubleProperty maxCellHeightProperty() { return maxCellHeight; }
  public final double getMaxCellHeight() { return maxCellHeight.get(); }

  private final Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
    }

    @Override
    protected void interpolate(double frac) {
      fractionalIndex = startFractionalIndex - startFractionalIndex * frac;

      getSkinnable().requestLayout();
    }
  };

  private double internalVisibleCellsCount;  // TODO must this be double?
  private double startFractionalIndex;
  private double fractionalIndex;

  public AbstractCarouselSkin(final TreeView<T> carousel) {
    super(carousel);

    getSkinnable().getStyleClass().add("carousel");

    InvalidationListener cellCountInvalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        getSkinnable().requestLayout();
      }
    };

    carousel.widthProperty().addListener(cellCountInvalidationListener);
    densityProperty().addListener(cellCountInvalidationListener);

    carousel.getFocusModel().focusedIndexProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {

        /*
         * Calculate at how many (fractional) items distance from the middle the carousel currently is and start the transition that will
         * move the now focused cell to the middle.
         */

        startFractionalIndex = fractionalIndex - old.doubleValue() + current.doubleValue();
        transition.playFromStart();
      }
    });
  }

  protected double getInternalVisibleCellsCount() {
    return internalVisibleCellsCount;
  }

  private void allocateAndSortCells() {
    getCellPool().reset();

    List<TreeCell<T>> children = new ArrayList<>();
    double cellCount = getSkinnable().getWidth() * getDensity();

    internalVisibleCellsCount = cellCount < 3 ? 3 : cellCount;

    int preferredCellCount = (int)internalVisibleCellsCount;

    int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();
    int index = selectedIndex - (int)Math.round(fractionalIndex);
    int start = index - (preferredCellCount - 1) / 2;
    int end = index + preferredCellCount / 2;

    double opacity = ((fractionalIndex > 0 ? fractionalIndex : 1 + fractionalIndex % 1) + 0.5) % 1;  // opacity for edge cells to achieve a gradual fade in/out

    for(int i = start; i <= end; i++) {
      if(i >= 0 && i <= getSkinnable().getExpandedItemCount()) {
        TreeCell<T> cell = getCellPool().getCell(i);

        children.add(cell);

        if(i == start) {
          cell.setOpacity(opacity);
        }
        else if(i == end) {
          cell.setOpacity(1.0 - opacity);
        }
        else {
          cell.setOpacity(1.0);
        }
      }
    }

    getCellPool().trim();

    /*
     * Sort the children and add them to the container.
     */

    Collections.sort(children, Z_ORDER_FRACTIONAL);

    getChildren().setAll(children);
  }

  private final Comparator<Node> Z_ORDER_FRACTIONAL = new Comparator<Node>() {
    @Override
    public int compare(Node o1, Node o2) {
      TreeCell<?> cell1 = (TreeCell<?>)o1;
      TreeCell<?> cell2 = (TreeCell<?>)o2;

      int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();
      int currentIndex = selectedIndex - (int)Math.round(fractionalIndex);

      int dist1 = Math.abs(cell1.getIndex() - currentIndex);
      int dist2 = Math.abs(cell2.getIndex() - currentIndex);

      return Integer.compare(dist2, dist1);
    }
  };

  @Override
  protected double computeMinWidth(double height) {
    return 16;
  }

  @Override
  protected double computeMinHeight(double width) {
    return 16;
  }

  @Override
  protected double computePrefWidth(double height) {
    return 16;
  }

  @Override
  protected double computePrefHeight(double width) {
    return 16;
  }

  /**
   * Returns the width and height of the cell when it is made to fit within
   * the MaxCellWidth and MaxCellHeight restrictions while preserving the aspect
   * ratio.
   *
   * @param cell a cell to calculate the dimensions for
   * @return the normalized dimensions
   */
  protected Dimension2D getNormalizedCellSize(TreeCell<T> cell) {
    double prefWidth = cell.prefWidth(-1);
    double prefHeight = cell.prefHeight(-1);

    if(prefWidth > getMaxCellWidth()) {
      prefHeight = prefHeight / prefWidth * getMaxCellWidth();
      prefWidth = getMaxCellWidth();
    }
    if(prefHeight > getMaxCellHeight()) {
      prefWidth = prefWidth / prefHeight * getMaxCellHeight();
      prefHeight = getMaxCellHeight();
    }

    return new Dimension2D(prefWidth, prefHeight);
  }

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    getSkinnable().setClip(new Rectangle(x, y, w, h));

    allocateAndSortCells();

    Shape cumulativeClip = null;
    int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();

    /*
     * Positions the Cells in front-to-back order.  This is done in order to clip the reflections
     * of cells positioned behind other cells using a cumulative clip.  Reflections would otherwise
     * blend with each other as they are partially transparent in nature.
     */

    ListIterator<Node> iterator = getChildren().listIterator(getChildren().size());

    while(iterator.hasPrevious()) {
      @SuppressWarnings("unchecked")
      TreeCell<T> cell = (TreeCell<T>)iterator.previous();

      cell.setVisible(!cell.isEmpty());

      if(!cell.isEmpty()) {
        Shape clip = applyEffectsToCellAndReturnClip(cell, selectedIndex - cell.getIndex() - fractionalIndex);

        layoutInArea(cell, w / 2, h / 2, cell.prefWidth(-1), cell.prefHeight(-1), 0, HPos.CENTER, VPos.CENTER);

        if(cumulativeClip != null) {
          Shape cellClip = Shape.intersect(cumulativeClip, new Rectangle(x, y, w, h));  // TODO there must be a better way to just copy a Shape...
          Point2D localToParent = cell.localToParent(0, 0);

          cellClip.getTransforms().add(new Translate(-localToParent.getX(), -localToParent.getY()));

          cell.setClip(cellClip);
        }
        else {
          cell.setClip(null);
        }

        if(clip != null) {
          clip.getTransforms().add(cell.getLocalToParentTransform());

          if(cumulativeClip == null) {
            cumulativeClip = new Rectangle(x, y, w, h);
          }

          cumulativeClip = Shape.subtract(cumulativeClip, clip);  // TODO a copy is made here...
        }
      }
    }
  }

  // index = fractional position on the carousel, not the model index
  public abstract Shape applyEffectsToCellAndReturnClip(TreeCell<T> cell, double index);
}
