package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.animation.Transition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.effect.PerspectiveTransform;
import javafx.util.Callback;
import javafx.util.Duration;

import com.sun.javafx.scene.control.skin.SkinBase;

public class CarouselSkin<T> extends SkinBase<Carousel<T>, CarouselBehavior<T>> {
  private final List<CarouselCell<T>> cells = new ArrayList<>();

  private final Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
    }

    @Override
    protected void interpolate(double frac) {
      fractionalIndex = startFractionalIndex - startFractionalIndex * frac;

      /*
       * Update the cell indices.
       */

      int index = getSkinnable().getFocusModel().getFocusedIndex() - (int)fractionalIndex;
      int visibleCellsCount = getSkinnable().getVisibleCellsCount();

      for(int i = index - (visibleCellsCount - 1) / 2; i < index + visibleCellsCount / 2; i++) {
        CarouselCell<T> carouselCell = cells.get((i + visibleCellsCount) % visibleCellsCount);

        carouselCell.updateIndex(i);
      }

      /*
       * Resort the children of the StackPane so the cells closest to center are on top.  A temporary list
       * is used to prevent events firing (and to avoid duplicate items in the Scene caused by the sorting steps).
       */

      List<Node> temporaryList = new ArrayList<>(getChildren());
      Collections.sort(temporaryList, Z_ORDER_FRAC);
      getChildren().setAll(temporaryList);

      doLayout();
    }
  };

  private double startFractionalIndex;
  private double fractionalIndex;

  public CarouselSkin(final Carousel<T> carousel) {
    super(carousel, new CarouselBehavior<>(carousel));

    getStyleClass().setAll("scroll-area");

//    carousel.itemsProperty().get().addListener(new ListChangeListener<Node>() {
//      @Override
//      public void onChanged(ListChangeListener.Change<? extends Node> change) {
//        System.out.println("List changed");
//        getChildren().clear();
//        getChildren().addAll(change.getList());
//      }
//    });

    // TODO loop (index -1 = last)

    for(int i = 0; i < carousel.getVisibleCellsCount(); i++) {
      CarouselCell<T> cell = createCell();

      cell.updateCarousel(carousel);
      cell.updateIndex(i);

      cell.currentDistanceToMiddle = 0;

      cells.add(cell);
      getChildren().add(cell);
    }

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

  private final Comparator<Node> Z_ORDER = new Comparator<Node>() {
    @Override
    public int compare(Node o1, Node o2) {
      int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();
      int dist1 = Math.abs(selectedIndex - ((CarouselCell<?>)o1).getIndex());
      int dist2 = Math.abs(selectedIndex - ((CarouselCell<?>)o2).getIndex());

      return Integer.compare(dist2, dist1);
    }
  };

  private final Comparator<Node> Z_ORDER_FRAC = new Comparator<Node>() {
    @Override
    public int compare(Node o1, Node o2) {
      int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();
      int dist1 = Math.abs(selectedIndex - ((CarouselCell<?>)o1).getIndex() - (int)Math.round(fractionalIndex));
      int dist2 = Math.abs(selectedIndex - ((CarouselCell<?>)o2).getIndex() - (int)Math.round(fractionalIndex));

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
    int selectedIndex = getSkinnable().getFocusModel().getFocusedIndex();
    double maxCellWidth = getWidth() * 0.2;

    for(Node child : getChildren()) {
      @SuppressWarnings("unchecked")
      CarouselCell<T> cell = (CarouselCell<T>)child;

      if(cell.isEmpty()) {
        cell.setVisible(false);
      }
      else {
        cell.setVisible(true);

        Style<T> style = new Ray<>(maxCellWidth, getHeight());

        double distanceFromMiddle = selectedIndex - cell.getIndex() - fractionalIndex;

        Point2D position = style.layoutCell(cell, distanceFromMiddle, getWidth(), getSkinnable().getVisibleCellsCount());

        layoutInArea(cell, getWidth() / 2 - position.getX(), getHeight() / 2 - position.getY(), 0, 0, 0, HPos.CENTER, VPos.CENTER);
      }
    }
  }


  public interface Style<T> {
    Point2D layoutCell(CarouselCell<T> cell, double distanceFromMiddle, double containerWidth, int visibleCellsCount);
  }

  public static class Bulge<T> implements Style<T> {
    private final double maxCellWidth;
    private final double maxCellHeight;

    public Bulge(double maxCellWidth, double maxCellHeight) {
      this.maxCellWidth = maxCellWidth;
      this.maxCellHeight = maxCellHeight;
    }

    @Override
    public Point2D layoutCell(CarouselCell<T> cell, double distanceFromMiddle, double containerWidth, int visibleCellsCount) {
      double cellWidth = cell.minWidth(-1);
      double cellHeight = cell.minHeight(-1);
      double scaleFactor = maxCellWidth / cellWidth;

      if(cellHeight * scaleFactor > maxCellHeight) {
        scaleFactor = maxCellHeight / cellHeight;
      }

      scaleFactor *= curve(distanceFromMiddle);

      cell.setScaleX(scaleFactor);
      cell.setScaleY(scaleFactor);

      return new Point2D(distanceFromMiddle * 50, 0);
    }

    private static double curve(double distanceFromCenter) {
      return 0.5 + 0.5 / (Math.abs(distanceFromCenter) + 1);
    }
  }

  public static class Ring<T> implements Style<T> {
    private final double maxCellWidth;
    private final double maxCellHeight;

    public Ring(double maxCellWidth, double maxCellHeight) {
      this.maxCellWidth = maxCellWidth;
      this.maxCellHeight = maxCellHeight;
    }

    @Override
    public Point2D layoutCell(CarouselCell<T> cell, double distanceFromMiddle, double containerWidth, int visibleCellsCount) {
      double cellWidth = cell.minWidth(-1);
      double cellHeight = cell.minHeight(-1);
      double scaleFactor = maxCellWidth / cellWidth;

      if(cellHeight * scaleFactor > maxCellHeight) {
        scaleFactor = maxCellHeight / cellHeight;
      }

      scaleFactor *= Math.cos(distanceFromMiddle / visibleCellsCount * 2 * Math.PI / 2);
      double position = Math.sin(distanceFromMiddle / visibleCellsCount * 2 * Math.PI / 2) * (containerWidth / 2 + maxCellWidth);

      cell.setScaleX(scaleFactor);
      cell.setScaleY(scaleFactor);

      return new Point2D(position, 0);
    }
  }

  public static class Ray<T> implements Style<T> {
    private final double maxCellWidth;
    private final double maxCellHeight;

    public Ray(double maxCellWidth, double maxCellHeight) {
      this.maxCellWidth = maxCellWidth;
      this.maxCellHeight = maxCellHeight;
    }

    @Override
    public Point2D layoutCell(CarouselCell<T> cell, double distanceFromMiddle, double containerWidth, int visibleCellsCount) {
      double cellWidth = cell.minWidth(-1);
      double cellHeight = cell.minHeight(-1);
      double scaleFactor = maxCellWidth / cellWidth;

      if(cellHeight * scaleFactor > maxCellHeight) {
        scaleFactor = maxCellHeight / cellHeight;
      }

      double angleOnCarousel = Math.PI / visibleCellsCount * distanceFromMiddle + Math.PI / 2;

      double carouselRadius = 250;
      double w = cell.minWidth(50);
      double h = cell.minHeight(50);

      Point3D ul = new Point3D((carouselRadius / scaleFactor + w / 2) * Math.cos(angleOnCarousel), -h / 2, (carouselRadius / scaleFactor + w / 2) * -Math.sin(angleOnCarousel));
      Point3D ur = new Point3D((carouselRadius / scaleFactor - w / 2) * Math.cos(angleOnCarousel), -h / 2, (carouselRadius / scaleFactor - w / 2) * -Math.sin(angleOnCarousel));
      Point3D ll = new Point3D((carouselRadius / scaleFactor + w / 2) * Math.cos(angleOnCarousel), h / 2, (carouselRadius / scaleFactor + w / 2) * -Math.sin(angleOnCarousel));
      Point3D lr = new Point3D((carouselRadius / scaleFactor - w / 2) * Math.cos(angleOnCarousel), h / 2, (carouselRadius / scaleFactor - w / 2) * -Math.sin(angleOnCarousel));

//  Equivalent code:
//      Point3D ul = new Point3D(carouselRadius / scaleFactor + w / 2, -h / 2, 0);
//      Point3D ur = new Point3D(carouselRadius / scaleFactor - w / 2, -h / 2, 0);
//      Point3D ll = new Point3D(carouselRadius / scaleFactor + w / 2, h / 2, 0);
//      Point3D lr = new Point3D(carouselRadius / scaleFactor - w / 2, h / 2, 0);
//
//      Point3D carouselAxis = new Point3D(0, 0, 0);
//
//      ul = rotateY(ul, carouselAxis, angleOnCarousel);
//      ur = rotateY(ur, carouselAxis, angleOnCarousel);
//      ll = rotateY(ll, carouselAxis, angleOnCarousel);
//      lr = rotateY(lr, carouselAxis, angleOnCarousel);

      if(distanceFromMiddle < 3 && distanceFromMiddle > -3) {
        double angle = Math.PI / 2 * -distanceFromMiddle/3 + Math.PI / 2;

        Point3D axis = new Point3D((ul.getX() + ur.getX()) / 2, 0, (ul.getZ() + ur.getZ()) / 2);
        ul = rotateY(ul, axis, angle);
        ur = rotateY(ur, axis, angle);
        ll = rotateY(ll, axis, angle);
        lr = rotateY(lr, axis, angle);
      }
      else if(distanceFromMiddle <= -3) {
        double angle = Math.PI;

        Point3D axis = new Point3D((ul.getX() + ur.getX()) / 2, 0, (ul.getZ() + ur.getZ()) / 2);

        ul = rotateY(ul, axis, angle);
        ur = rotateY(ur, axis, angle);
        ll = rotateY(ll, axis, angle);
        lr = rotateY(lr, axis, angle);
      }

      // projection
      double viewDistance = containerWidth / scaleFactor;
      double fov = 512 / scaleFactor;
      double cw = w / 2;
      double ch = h / 2;

      Point2D ul2d = project(ul, viewDistance, fov, cw, ch);
      Point2D ur2d = project(ur, viewDistance, fov, cw, ch);
      Point2D ll2d = project(ll, viewDistance, fov, cw, ch);
      Point2D lr2d = project(lr, viewDistance, fov, cw, ch);

      double position = Math.sin(distanceFromMiddle / visibleCellsCount * 2 * Math.PI / 2) * (containerWidth / 2 + maxCellWidth);

      cell.setScaleX(scaleFactor);
      cell.setScaleY(scaleFactor);

      cell.setEffect(new PerspectiveTransform(ul2d.getX(), ul2d.getY(), ur2d.getX(), ur2d.getY(), lr2d.getX(), lr2d.getY(), ll2d.getX(), ll2d.getY()));

      return new Point2D(position, 0);
    }

    private static Point2D project(Point3D p, double viewDistance, double fov, double cw, double ch) {
      return new Point2D(p.getX() * fov / (p.getZ() + viewDistance) + cw, p.getY() * fov / (p.getZ() + viewDistance) + ch);
    }

    private static Point3D rotateY(Point3D p, Point3D axis, double radians) {
      Point3D input = new Point3D(p.getX() - axis.getX(), p.getY() - axis.getY(), p.getZ() - axis.getZ());

      return new Point3D(
        input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + axis.getX(),
        input.getY() + axis.getY(),
        input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + axis.getZ()
      );
    }
  }
}
