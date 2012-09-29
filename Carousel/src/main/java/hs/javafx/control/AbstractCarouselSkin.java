package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Translate;
import javafx.util.Callback;
import javafx.util.Duration;

import com.sun.javafx.scene.control.skin.SkinBase;

public abstract class AbstractCarouselSkin<T> extends SkinBase<Carousel<T>, CarouselBehavior<T>> {
  private final ArrayList<CarouselCell<T>> cells = new ArrayList<>();

  private double visibleCellsCount;

  protected double getVisibleCellsCount() {
    return visibleCellsCount;
  }


  private final Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
    }

    @Override
    protected void interpolate(double frac) {
      fractionalIndex = startFractionalIndex - startFractionalIndex * frac;

      sortChildren();
      doLayout();
    }
  };

  private void sortChildren() {

    /*
     * Update the cell indices.
     */

    int index = getSkinnable().getFocusModel().getFocusedIndex() - (int)Math.round(fractionalIndex);
    int visibleCellsCount = cells.size();
    int start = index - (visibleCellsCount - 1) / 2;
    int end = index + visibleCellsCount / 2;

    double opacity = ((fractionalIndex > 0 ? fractionalIndex : 1 + fractionalIndex % 1) + 0.5) % 1;

    for(int i = start; i <= end; i++) {
      CarouselCell<T> carouselCell = cells.get((i + visibleCellsCount) % visibleCellsCount);

      carouselCell.updateIndex(i);

      if(i == start) {
        carouselCell.setOpacity(opacity);
      }
      else if(i == end) {
        carouselCell.setOpacity(1.0 - opacity);
      }
      else {
        carouselCell.setOpacity(1.0);
      }
    }

    /*
     * Resort the children of the StackPane so the cells closest to center are on top.  A temporary list
     * is used to prevent events firing (and to avoid duplicate items in the Scene caused by the sorting steps).
     */
    // TODO perhaps this is possible to achieve with toFront() ?

    List<Node> temporaryList = new ArrayList<>(getChildren());
    Collections.sort(temporaryList, Z_ORDER_FRAC);
    getChildren().setAll(temporaryList);
  }

  private double startFractionalIndex;
  private double fractionalIndex;

  public AbstractCarouselSkin(final Carousel<T> carousel) {
    super(carousel, new CarouselBehavior<>(carousel));

    getStyleClass().setAll("scroll-area");

    InvalidationListener cellCountInvalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        allocateCells();
        sortChildren();
      }
    };

    carousel.widthProperty().addListener(cellCountInvalidationListener);
    carousel.densityProperty().addListener(cellCountInvalidationListener);

    allocateCells();

    carousel.getFocusModel().focusedIndexProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {

        /*
         * Calculate at how many (fractional) items distance from the middle the carousel currently is and start the transistion that will
         * move the now focused cell to the middle.
         */

        startFractionalIndex = fractionalIndex - old.doubleValue() + current.doubleValue();
        transition.playFromStart();
      }
    });
  }

  // Goal: Spacings between Cells should remain similar when width changes
  private void allocateCells() {
    double widthFactor = getSkinnable().getDensity();

    visibleCellsCount = getSkinnable().getWidth() * widthFactor;
    visibleCellsCount = visibleCellsCount < 3 ? 3 : visibleCellsCount;

    int preferredCellCount = (int)visibleCellsCount;

    if(cells.size() > preferredCellCount) {
      List<CarouselCell<T>> cellsToBeDeleted = cells.subList(preferredCellCount, cells.size());

      for(CarouselCell<T> carouselCell : cellsToBeDeleted) {
        carouselCell.updateIndex(-1);
      }

      getChildren().removeAll(cellsToBeDeleted);
      cellsToBeDeleted.clear();
    }
    else if(cells.size() < preferredCellCount) {
      for(int i = cells.size(); i < preferredCellCount; i++) {
        CarouselCell<T> cell = createCell();

        cell.updateCarousel(getSkinnable());
        cell.updateIndex(i);

        cell.currentDistanceToMiddle = 0;

        cells.add(cell);
        getChildren().add(cell);
      }
    }
  }

  private final Comparator<Node> Z_ORDER_FRAC = new Comparator<Node>() {
    @Override
    public int compare(Node o1, Node o2) {
      CarouselCell<?> cell1 = (CarouselCell<?>)o1;
      CarouselCell<?> cell2 = (CarouselCell<?>)o2;

      int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();

      int dist1 = Math.abs(selectedIndex - cell1.getIndex() - (int)Math.round(fractionalIndex));
      int dist2 = Math.abs(selectedIndex - cell2.getIndex() - (int)Math.round(fractionalIndex));

      return Integer.compare(dist2, dist1);
    }
  };

  private CarouselCell<T> createCell() {
    Callback<Carousel<T>, CarouselCell<T>> cellFactory = getSkinnable().getCellFactory();

    if(cellFactory == null) {
      return new CarouselCell<T>() {
        @Override
        protected void updateItem(T item, boolean empty) {
          super.updateItem(item, empty);

          if(!empty) {
            setText(item.toString());
          }
        }
      };
    }

    return cellFactory.call(getSkinnable());
  }

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

  @Override
  protected void layoutChildren() {
    doLayout();
  }

  private void doLayout() {
    Shape currentClip = new Rectangle(0, 0, getWidth(), getHeight());
    int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();

    List<Node> children = new ArrayList<>(getChildren());

    Collections.reverse(children);

    for(Node child : children) {
      @SuppressWarnings("unchecked")
      CarouselCell<T> cell = (CarouselCell<T>)child;

      if(cell.isEmpty()) {
        cell.setVisible(false);
      }
      else {
        cell.setVisible(true);

        double distanceFromMiddle = selectedIndex - cell.getIndex() - fractionalIndex;

        Point2D position = positionCell(cell, distanceFromMiddle, cell.getMaxCellWidth(), cell.getMaxCellHeight());
        Shape c = layoutCell(cell, distanceFromMiddle, cell.getMaxCellWidth(), cell.getMaxCellHeight());

        layoutInArea(child, getWidth() / 2 - position.getX(), getHeight() / 2 - position.getY(), 0, 0, 0, HPos.CENTER, VPos.CENTER);

        Shape newClip;
        if(c != null) {
          c.getTransforms().addAll(cell.getLocalToParentTransform());
          c.getTransforms().addAll(cell.getParent().getLocalToParentTransform());
          newClip = Shape.subtract(currentClip, c);
        }
        else {
          newClip = Shape.intersect(currentClip, new Rectangle(0, 0, getWidth(), getHeight()));  // TODO there must be a better way to just copy a Shape...
        }

        Point2D localToParent = cell.localToParent(0, 0);
        localToParent = cell.getParent().localToParent(localToParent);

        currentClip.getTransforms().add(new Translate(-localToParent.getX(), -localToParent.getY()));

        cell.setClip(currentClip);

        currentClip = newClip;
      }
    }

    setClip(new Rectangle(0, 0, getWidth(), getHeight()));
  }

  // index = fractional index
  public abstract Shape layoutCell(CarouselCell<T> cell, double index, double maxCellWidth, double maxCellHeight);
  public abstract Point2D positionCell(CarouselCell<T> cell, double index, double maxCellWidth, double maxCellHeight);
}
