package hs.javafx.demo;

import hs.javafx.carousel.AbstractCarouselSkin;
import hs.javafx.carousel.FlatCarouselSkin;
import hs.javafx.carousel.RayCarouselSkin;
import hs.javafx.carousel.RibbonCarouselSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ControlPanel extends VBox {
  private static final ObservableList<SkinFactory<TreeView<Object>>> SKIN_FACTORIES = FXCollections.observableArrayList();

  static {
    SKIN_FACTORIES.add(new SkinFactory<TreeView<Object>>() {
      @Override
      public Skin<?> createSkin(TreeView<Object> skinnable) {
        return new RayCarouselSkin<>(skinnable);
      }

      @Override
      public String toString() {
        return "Ray Skin";
      }
    });

    SKIN_FACTORIES.add(new SkinFactory<TreeView<Object>>() {
      @Override
      public Skin<?> createSkin(TreeView<Object> skinnable) {
        return new RayCarouselSkin<Object>(skinnable) {
          @Override
          protected void preLayoutCustomizeCell(RayLayoutItem item) {
//            double angleOnCarousel = iterator.getAngleOnCarousel();
//            System.out.println("angleOnCarousel = " + angleOnCarousel);  // 0 = center cell
//            double bellWidth = 0.2;
//            double g = Math.pow(Math.E, -(angleOnCarousel * angleOnCarousel) / Math.pow(2 * bellWidth, 2)) + 1;
//            iterator.setAngleOnCarousel(angleOnCarousel * g);
          }

          @Override
          protected void postLayoutCustomizeCell(RayLayoutItem item) {
            Point3D[] points = item.getCoordinates();
            Point3D center = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);

            for(int i = 0; i < points.length; i++) {
              points[i] = rotateY(points[i], center, Math.PI * 0.5);
            }
          }
        };
      }

      @Override
      public String toString() {
        return "Ray Skin (customized, circular)";
      }
    });

    SKIN_FACTORIES.add(new SkinFactory<TreeView<Object>>() {
      @Override
      public Skin<?> createSkin(TreeView<Object> skinnable) {
        return new FlatCarouselSkin<>(skinnable);
      }

      @Override
      public String toString() {
        return "Flat Skin";
      }
    });

    SKIN_FACTORIES.add(new SkinFactory<TreeView<Object>>() {
      @Override
      public Skin<?> createSkin(TreeView<Object> skinnable) {
        return new RibbonCarouselSkin<>(skinnable);
      }

      @Override
      public String toString() {
        return "Ribbon Skin";
      }
    });
  }

  private final TreeView<Object> skinnable;

  private GridPane optionGridPane = new GridPane();

  @SuppressWarnings("unchecked")
  public ControlPanel(TreeView<?> skinnable) {
    this.skinnable = (TreeView<Object>)skinnable;

    setPadding(new Insets(20.0));

    final ComboBox<SkinFactory<TreeView<Object>>> comboBox = new ComboBox<>(SKIN_FACTORIES);

    comboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SkinFactory<TreeView<Object>>>() {
      @Override
      public void changed(ObservableValue<? extends SkinFactory<TreeView<Object>>> observableValue, SkinFactory<TreeView<Object>> old, SkinFactory<TreeView<Object>> current) {
        System.out.println(">>> Setting new Skin");
        skinnable.setSkin(current.createSkin(ControlPanel.this.skinnable));
        fillOptionGridPane((AbstractCarouselSkin<?>)skinnable.getSkin());
      }
    });

    skinnable.skinProperty().addListener(new ChangeListener<Skin<?>>() {

      @Override
      public void changed(ObservableValue<? extends Skin<?>> observable, Skin<?> oldValue, Skin<?> newValue) {
        System.out.println("Skin changed from: " + oldValue + " to " + newValue);
      }

    });

    comboBox.getSelectionModel().select(0);

    getChildren().addAll(optionGridPane, comboBox);
  }

  ChangeListener<Number> viewAlignmentListener = new ChangeListener<Number>() {
    @Override
    public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {
      AbstractCarouselSkin<?> carouselSkin = (AbstractCarouselSkin<?>)skinnable.getSkin();

      double f = carouselSkin.getMaxCellHeight() / carouselSkin.getSkinnable().getHeight();
      double c = ((1.0 - f) / 2 + current.doubleValue() * f) * 100;
      carouselSkin.getSkinnable().setStyle(String.format("-fx-background-color: linear-gradient(to bottom, black 0%%, black %6.2f%%, grey %6.2f%%, black)", c, c));
    }
  };

  public void fillOptionGridPane(final AbstractCarouselSkin<?> skin) {
    row = 0;
    optionGridPane.getChildren().clear();

    addSlider(skin.cellAlignmentProperty(), "%4.2f", "Cell Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of cells which donot utilize all of the maximum available height");
    addSlider(skin.densityProperty(), "%6.4f", "Cell Density (0.1 - 3.0)", 0.1, 3.0, 1.1, "The density of the cells, higher values cause cells to overlap, lower values space cells out more");
    addSlider(skin.maxCellWidthProperty(), "%4.0f", "Maximum Cell Width (1 - 2000)", 1, 1000, 5, "The maximum width a cell is allowed to become");
    addSlider(skin.maxCellHeightProperty(), "%4.0f", "Maximum Cell Height (1 - 2000)", 1, 1000, 5, "The maximum height a cell is allowed to become");

    optionGridPane.add(new HBox() {{
      setSpacing(20);
      getChildren().add(new CheckBox("Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(skin.reflectionsEnabledProperty());
      }});
      getChildren().add(new CheckBox("Clip Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(skin.clipReflectionsProperty());
      }});
    }}, 2, row++);

    if(skin instanceof RayCarouselSkin) {
      RayCarouselSkin<?> rayCarouselSkin = (RayCarouselSkin<?>)skin;

      rayCarouselSkin.viewAlignmentProperty().addListener(viewAlignmentListener);
      viewAlignmentListener.changed(rayCarouselSkin.viewAlignmentProperty(), rayCarouselSkin.viewAlignmentProperty().getValue(), rayCarouselSkin.viewAlignmentProperty().getValue());

      addSlider(rayCarouselSkin.radiusRatioProperty(), "%4.2f", "Radius Ratio (0.0 - 2.0)", 0.0, 2.0, 0.1, "The radius of the carousel expressed as the fraction of half the view's width");
      addSlider(rayCarouselSkin.viewDistanceRatioProperty(), "%4.2f", "View Distance Ratio (0.0 - 4.0)", 0.0, 4.0, 0.1, "The distance of the camera expressed as a fraction of the radius of the carousel");
      addSlider(rayCarouselSkin.carouselViewFractionProperty(), "%4.2f", "Carousel View Fraction (0.0 - 1.0)", 0.0, 1.0, 0.1, "The portion of the carousel that is used for displaying cells");
      addSlider(rayCarouselSkin.viewAlignmentProperty(), "%4.2f", "View Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of the camera with respect to the carousel");
    }
    else {
      skin.getSkinnable().setStyle(String.format("-fx-background-color: linear-gradient(to bottom, black 0%%, black %6.2f%%, grey %6.2f%%, black)", 50.0, 50.0));
    }
  }

  private int row = 1;

  private void addSlider(final DoubleProperty property, final String format, String description, double min, double max, final double increment, String longDescription) {
    optionGridPane.add(new Label(description) {{
      setStyle("-fx-font-size: 16px");
    }}, 1, row);
    optionGridPane.add(new Slider(min, max, property.get()) {{
      setStyle("-fx-font-size: 16px");
      property.bind(valueProperty());
      setBlockIncrement(increment);
      setMinWidth(400);
    }}, 2, row);
    optionGridPane.add(new Label() {{
      setStyle("-fx-font-size: 16px");
      textProperty().bind(property.asString(format));
    }}, 3, row);

    row++;

    optionGridPane.add(new Label(longDescription) {{
      setPadding(new Insets(0, 0, 5, 0));
    }}, 1, row, 3, 1);

    row++;
  }

  private interface SkinFactory<T> {
    Skin<?> createSkin(T skinnable);
  }
}
