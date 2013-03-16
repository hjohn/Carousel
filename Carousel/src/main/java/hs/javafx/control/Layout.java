package hs.javafx.control;

public interface Layout {
  CellIterator renderCellIterator(double fractionalIndex);
  CarouselSkin<?> getSkin();
}
