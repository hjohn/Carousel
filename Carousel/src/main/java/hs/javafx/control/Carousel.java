package hs.javafx.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.util.Callback;

public class Carousel<T> extends Control {
  private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<>();
  public final ObjectProperty<ObservableList<T>> itemsProperty() { return items; }
  public final ObservableList<T> getItems() { return items.get(); }

  private final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(Orientation.HORIZONTAL);
  public final ObjectProperty<Orientation> orientationProperty() { return orientation; }
  public final Orientation getOrientation() { return orientation.get(); }

  private final ObjectProperty<Callback<Carousel<T>, CarouselCell<T>>> cellFactory = new SimpleObjectProperty<>();
  public final ObjectProperty<Callback<Carousel<T>, CarouselCell<T>>> cellFactoryProperty() { return cellFactory; }
  public final Callback<Carousel<T>, CarouselCell<T>> getCellFactory() { return cellFactory.get(); }

  private final ObjectProperty<FocusModel<T>> focusModel = new SimpleObjectProperty<>();
  public final ObjectProperty<FocusModel<T>> focusModelProperty() { return focusModel; }
  public final FocusModel<T> getFocusModel() { return focusModel.get(); }

  private final IntegerProperty visibleCellsCount = new SimpleIntegerProperty(30);
  public final IntegerProperty visibleCellsCountProperty() { return visibleCellsCount; }
  public final int getVisibleCellsCount() { return visibleCellsCount.get(); }

  public Carousel() {
    getStyleClass().setAll("carousel");

    ObservableList<T> observableArrayList = FXCollections.observableArrayList();

    items.set(observableArrayList);
    focusModel.set(new CarouselFocusModel<>(this));
  }

  @Override
  protected String getUserAgentStylesheet() {
    return getClass().getResource("Carousel.css").toExternalForm();
  }

  static class CarouselFocusModel<T> extends FocusModel<T> {
    private final Carousel<T> carousel;

    private ChangeListener<ObservableList<T>> itemsListener = new ChangeListener<ObservableList<T>>() {
      @Override
      public void changed(ObservableValue<? extends ObservableList<T>> observableValue, ObservableList<T> old, ObservableList<T> current) {
        updateItemsObserver(current, current);
      }
    };

    private WeakChangeListener<ObservableList<T>> weakItemsListener = new WeakChangeListener<>(this.itemsListener);

    private final ListChangeListener<T> itemsContentListener = new ListChangeListener<T>() {
      @Override
      public void onChanged(ListChangeListener.Change<? extends T> change) {
        change.next();

        int i = change.getFrom();
        if(getFocusedIndex() == -1 || i > getFocusedIndex()) {
          return;
        }

        change.reset();
        boolean bool1 = false;
        boolean bool2 = false;
        int j = 0;
        int k = 0;
        while(change.next()) {
          bool1 |= change.wasAdded();
          bool2 |= change.wasRemoved();
          j += change.getAddedSize();
          k += change.getRemovedSize();
        }

        if((bool1) && (!bool2)) {
          focus(getFocusedIndex() + j);
        }
        else if((!bool1) && (bool2)) {
          focus(getFocusedIndex() - k);
        }
      }
    };

    private WeakListChangeListener<T> weakItemsContentListener = new WeakListChangeListener<>(this.itemsContentListener);

    public CarouselFocusModel(Carousel<T> carousel) {
      if(carousel == null) {
        throw new IllegalArgumentException("Carousel can not be null");
      }

      this.carousel = carousel;
      this.carousel.itemsProperty().addListener(this.weakItemsListener);
      if(carousel.getItems() != null) {
        this.carousel.getItems().addListener(this.weakItemsContentListener);
      }
    }

    private void updateItemsObserver(ObservableList<T> oldList, ObservableList<T> currentList) {
      if(oldList != null) {
        oldList.removeListener(this.weakItemsContentListener);
      }
      if(currentList != null) {
        currentList.addListener(this.weakItemsContentListener);
      }
    }

    @Override
    protected int getItemCount() {
      return isEmpty() ? -1 : carousel.getItems().size();
    }

    @Override
    protected T getModelItem(int paramInt) {
      if(isEmpty() || paramInt < 0 || paramInt >= getItemCount()) {
        return null;
      }

      return carousel.getItems().get(paramInt);
    }

    private boolean isEmpty() {
      return carousel == null || carousel.getItems() == null;
    }
  }
}
