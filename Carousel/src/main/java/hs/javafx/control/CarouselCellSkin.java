package hs.javafx.control;

import com.sun.javafx.scene.control.skin.CellSkinBase;

public class CarouselCellSkin<T> extends CellSkinBase<CarouselCell<T>, CarouselCellBehavior<T>> {
  public CarouselCellSkin(CarouselCell<T> cell) {
    super(cell, new CarouselCellBehavior<>(cell));
  }
}
