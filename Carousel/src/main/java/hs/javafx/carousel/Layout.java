package hs.javafx.carousel;

public interface Layout {
  CellIterator renderCellIterator(double fractionalIndex);
  CarouselSkin<?> getSkin();
}
