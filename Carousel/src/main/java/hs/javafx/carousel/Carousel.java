package hs.javafx.carousel;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class Carousel<T> extends TreeView<T> {
  private static final String DEFAULT_STYLE_CLASS = "carousel";

  public Carousel(TreeItem<T> root) {
    super(root);



//    getStylesheets().add(getClass().getResource("carousel.css").toExternalForm());
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  public Carousel() {
    this(null);
  }

  @Override
  protected String getUserAgentStylesheet() {
    return getClass().getResource("Carousel.css").toExternalForm();
  }
}
