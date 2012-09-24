package hs.javafx.control;

import java.lang.ref.WeakReference;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.IndexedCell;

public class CarouselCell<T> extends IndexedCell<T> {
  double startDistanceToMiddle;
  double currentDistanceToMiddle;

  public CarouselCell() {
    getStyleClass().setAll("carousel-cell");

    indexProperty().addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        updateItem();
      }
    });
  }

  private final ListChangeListener<T> itemsListener = new ListChangeListener<T>() {
    @Override
    public void onChanged(ListChangeListener.Change<? extends T> change) {
      updateItem();
    }
  };

  private final WeakListChangeListener<T> weakItemsListener = new WeakListChangeListener<>(this.itemsListener);

  private ReadOnlyObjectWrapper<Carousel<T>> carousel = new ReadOnlyObjectWrapper<Carousel<T>>(this, "carousel") {
    private WeakReference<Carousel<T>> carouselRef = new WeakReference<>(null);

    @Override
    protected void invalidated() {
      Carousel<T> carousel = get();
      Carousel<T> oldCarousel = carouselRef.get();

      if (carousel == oldCarousel) {
        return;
      }

      ObservableList<T> list;

      if(oldCarousel != null) {
        list = oldCarousel.getItems();

        if(list != null) {
          list.removeListener(weakItemsListener);
        }
      }

      if(carousel != null) {
        list = carousel.getItems();

        if(list != null) {
          list.addListener(weakItemsListener);
        }

        this.carouselRef = new WeakReference<>(carousel);
      }

      updateItem();
    }
  };

  public final Carousel<T> getCarousel() { return carousel.get(); }
  public final ReadOnlyObjectProperty<Carousel<T>> carouselProperty() { return carousel.getReadOnlyProperty(); }

  private void updateItem() {
    Carousel<T> carousel = getCarousel();
    ObservableList<T> list = carousel == null ? null : carousel.getItems();

    if(list != null && getIndex() >= 0 && getIndex() < list.size()) {
      T item = list.get(getIndex());

      if(item == null || !item.equals(getItem())) {
        System.out.println("Updating item: " + item);
        updateItem(item, false);
      }
    }
    else {
      updateItem(null, true);
    }
  }

  public final void updateCarousel(Carousel<T> carousel) {
    this.carousel.set(carousel);
  }
}
