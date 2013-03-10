package hs.javafx.control;

public interface Layout<T> {
  CellIterator<T> renderCellIterator(double fractionalIndex);
  CarouselSkin<T> getSkin();
}
