package hs.javafx.control;

public class LinearLayout<T> implements Layout<T> {
  private final CarouselSkin<T> skin;

  public LinearLayout(CarouselSkin<T> skin) {
    this.skin = skin;
  }

  @Override
  public CarouselSkin<T> getSkin() {
    return skin;
  }

  @Override
  public CellIterator<T> renderCellIterator(double fractionalIndex) {
    return new LinearCellIterator<>(this, fractionalIndex, false);
  }

}
