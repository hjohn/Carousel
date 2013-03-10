package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

public class CarouselSkin<T> extends AbstractTreeViewSkin<T> {
  private final DoubleProperty cellAlignment = new SimpleDoubleProperty(0.8);
  public final DoubleProperty cellAlignmentProperty() { return cellAlignment; }
  public final double getCellAlignment() { return cellAlignment.get(); }

  private final BooleanProperty reflectionEnabled = new SimpleBooleanProperty(true);
  public final BooleanProperty reflectionEnabledProperty() { return reflectionEnabled; }
  public final boolean getReflectionEnabled() { return reflectionEnabled.get(); }

  private final BooleanProperty clipReflections = new SimpleBooleanProperty(true);
  public final BooleanProperty clipReflectionsProperty() { return clipReflections; }
  public final boolean getClipReflections() { return clipReflections.get(); }

  private final DoubleProperty density = new SimpleDoubleProperty(0.02);
  public final DoubleProperty densityProperty() { return density; }
  public final double getDensity() { return density.get(); }

  private final DoubleProperty maxCellWidth = new SimpleDoubleProperty(300);
  public final DoubleProperty maxCellWidthProperty() { return maxCellWidth; }
  public final double getMaxCellWidth() { return maxCellWidth.get(); }

  private final DoubleProperty maxCellHeight = new SimpleDoubleProperty(200);
  public final DoubleProperty maxCellHeightProperty() { return maxCellHeight; }
  public final double getMaxCellHeight() { return maxCellHeight.get(); }

  private final ObjectProperty<Layout<T>> layout = new SimpleObjectProperty<Layout<T>>(new RayLayout<>(this));
  public ObjectProperty<Layout<T>> layoutProperty() { return layout; }
  public Layout<T> getLayout() { return layout.get(); }
  public void setLayout(Layout<T> layout) { this.layout.set(layout); }

  private Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
      setInterpolator(Interpolator.LINEAR);  // frequently restarted animations work very poorly with non-linear Interpolators
    }

    @Override
    protected void interpolate(double frac) {
      fractionalIndex = startFractionalIndex - startFractionalIndex * frac;
      getSkinnable().requestLayout();
    }
  };

  private double startFractionalIndex;
  private double fractionalIndex;

  public CarouselSkin(final TreeView<T> carousel) {
    super(carousel);

    getSkinnable().getStyleClass().add("carousel");

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        getSkinnable().requestLayout();
      }
    };

    carousel.widthProperty().addListener(invalidationListener);
    densityProperty().addListener(invalidationListener);

    cellAlignmentProperty().addListener(invalidationListener);
    reflectionEnabledProperty().addListener(invalidationListener);
    clipReflectionsProperty().addListener(invalidationListener);
    maxCellWidthProperty().addListener(invalidationListener);
    maxCellHeightProperty().addListener(invalidationListener);

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

  protected final Comparator<Node> Z_ORDER_FRACTIONAL = new Comparator<Node>() {
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

//  protected abstract Iterator<CellTransformClipTuple<T>> renderCellIterator(double fractionalIndex);

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    getSkinnable().setClip(new Rectangle(x, y, w, h));

    getCellPool().reset();
    getChildren().clear();

    Shape cumulativeClip = null;

    /*
     * Positions the Cells in front-to-back order.  This is done in order to clip the reflections
     * of cells positioned behind other cells using a cumulative clip.  Reflections would otherwise
     * blend with each other as they are partially transparent in nature.
     */

    CellIterator<T> iterator = getLayout().renderCellIterator(fractionalIndex);

    while(iterator.hasNext()) {
      TreeCell<T> cell = iterator.next();
      Shape clip = iterator.getClip();

      layoutInArea(cell, w / 2, h / 2, cell.prefWidth(-1), cell.prefHeight(-1), 0, HPos.CENTER, VPos.CENTER);

      cell.setClip(cumulativeClip);

      if(clip != null) {
        if(cumulativeClip == null) {
          cumulativeClip = new Rectangle(x - w / 2, y - h / 2, w, h);
        }

        cumulativeClip = Shape.subtract(cumulativeClip, clip);
      }
      else if(cumulativeClip != null) {
        cumulativeClip = Shape.union(cumulativeClip, cumulativeClip);  // Makes a copy as the same clip cannot be part of a scenegraph twice
      }
    }

    getCellPool().trim();

    /*
     * Sort the children and re-add them to the container.
     */

    List<Node> children = new ArrayList<>(getChildren());

    Collections.sort(children, Z_ORDER_FRACTIONAL);

    getChildren().setAll(children);
  }
}
