package hs.javafx.control;

public class LinearLayout implements Layout {
  private final CarouselSkin<?> skin;

  public LinearLayout(CarouselSkin<?> skin) {
    this.skin = skin;
  }

  @Override
  public CarouselSkin<?> getSkin() {
    return skin;
  }

  @Override
  public CellIterator renderCellIterator(double fractionalIndex) {
    return new LinearCellIterator(this, fractionalIndex, false);
  }
}
