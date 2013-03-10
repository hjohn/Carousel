package hs.javafx.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class RayLayout<T> implements Layout<T> {
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

  private final CarouselSkin<T> skin;

  public RayLayout(final CarouselSkin<T> skin) {
    this.skin = skin;

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        skin.getSkinnable().requestLayout();
      }
    };

    radiusRatioProperty().addListener(invalidationListener);
    viewDistanceRatioProperty().addListener(invalidationListener);
    viewAlignmentProperty().addListener(invalidationListener);
    carouselViewFractionProperty().addListener(invalidationListener);
  }

  @Override
  public CellIterator<T> renderCellIterator(double fractionalIndex) {
    return new RayCellIterator<>(this, fractionalIndex);
  }

  @Override
  public CarouselSkin<T> getSkin() {
    return skin;
  }
}
