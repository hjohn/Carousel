package hs.javafx.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ControlPanel extends VBox {
  private final CarouselSkin<?> carouselSkin;

  private GridPane optionGridPane = new GridPane();

  public ControlPanel(final CarouselSkin<?> carouselSkin) {
    this.carouselSkin = carouselSkin;

    setPadding(new Insets(20.0));

    final ComboBox<Layout> comboBox = new ComboBox<>(FXCollections.observableArrayList(
      new RayLayout(carouselSkin) {
        @Override
        public String toString() {
          return "Ray Layout";
        }
      },
      new RayLayout(carouselSkin) {
        @Override
        protected void customizeCell(RayCellIterator iterator) {
          Point3D[] points = iterator.currentPoints();
          Point3D center = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);

          for(int i = 0; i < points.length; i++) {
            points[i] = rotateY(points[i], center, Math.PI * 0.5);
          }

          this.fadeOutEdgeCells(iterator, 0.5);
        }

        @Override
        public String toString() {
          return "Ray Layout (circular)";
        }
      },
      new LinearLayout(carouselSkin) {
        @Override
        public String toString() {
          return "Linear Layout";
        }
      }
    ));

    comboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Layout>() {
      @Override
      public void changed(ObservableValue<? extends Layout> observableValue, Layout old, Layout current) {
        if(old != null) {
          unbind(old);
        }
        fillOptionGridPane(carouselSkin, current);
      }
    });

    comboBox.getSelectionModel().select(0);
    carouselSkin.layoutProperty().bind(comboBox.getSelectionModel().selectedItemProperty());

    getChildren().addAll(optionGridPane, comboBox);
  }

  ChangeListener<Number> viewAlignmentListener = new ChangeListener<Number>() {
    @Override
    public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {
      double f = carouselSkin.getMaxCellHeight() / carouselSkin.getSkinnable().getHeight();
      double c = ((1.0 - f) / 2 + current.doubleValue() * f) * 100;
      carouselSkin.getSkinnable().setStyle(String.format("-fx-background-color: linear-gradient(to bottom, black 0%%, black %6.2f%%, grey %6.2f%%, black)", c, c));
    }
  };

  public void unbind(Layout genericLayout) {
    if(genericLayout instanceof RayLayout) {
      RayLayout layout = (RayLayout)genericLayout;

      layout.radiusRatioProperty().unbind();
      layout.viewDistanceRatioProperty().unbind();
      layout.carouselViewFractionProperty().unbind();
      layout.viewAlignmentProperty().unbind();
      layout.viewAlignmentProperty().removeListener(viewAlignmentListener);
    }
  }

  public void fillOptionGridPane(final CarouselSkin<?> skin, Layout genericLayout) {
    row = 0;
    optionGridPane.getChildren().clear();

    addSlider(skin.cellAlignmentProperty(), "%4.2f", "Cell Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of cells which donot utilize all of the maximum available height");
    addSlider(skin.densityProperty(), "%6.4f", "Cell Density (0.001 - 0.1)", 0.001, 0.1, 0.0025, "The density of cells in cells per pixel of view width");
    addSlider(skin.maxCellWidthProperty(), "%4.0f", "Maximum Cell Width (1 - 2000)", 1, 1000, 5, "The maximum width a cell is allowed to become");
    addSlider(skin.maxCellHeightProperty(), "%4.0f", "Maximum Cell Height (1 - 2000)", 1, 1000, 5, "The maximum height a cell is allowed to become");

    optionGridPane.add(new HBox() {{
      setSpacing(20);
      getChildren().add(new CheckBox("Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(skin.reflectionEnabledProperty());
      }});
      getChildren().add(new CheckBox("Clip Reflections?") {{
        setStyle("-fx-font-size: 16px");
        selectedProperty().bindBidirectional(skin.clipReflectionsProperty());
      }});
    }}, 2, row++);

    if(genericLayout instanceof RayLayout) {
      RayLayout layout = (RayLayout)genericLayout;

      layout.viewAlignmentProperty().addListener(viewAlignmentListener);
      viewAlignmentListener.changed(layout.viewAlignmentProperty(), layout.viewAlignmentProperty().getValue(), layout.viewAlignmentProperty().getValue());

      addSlider(layout.radiusRatioProperty(), "%4.2f", "Radius Ratio (0.0 - 2.0)", 0.0, 2.0, 0.1, "The radius of the carousel expressed as the fraction of half the view's width");
      addSlider(layout.viewDistanceRatioProperty(), "%4.2f", "View Distance Ratio (0.0 - 4.0)", 0.0, 4.0, 0.1, "The distance of the camera expressed as a fraction of the radius of the carousel");
      addSlider(layout.carouselViewFractionProperty(), "%4.2f", "Carousel View Fraction (0.0 - 1.0)", 0.0, 1.0, 0.1, "The portion of the carousel that is used for displaying cells");
      addSlider(layout.viewAlignmentProperty(), "%4.2f", "View Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of the camera with respect to the carousel");
    }
    else {
      carouselSkin.getSkinnable().setStyle(String.format("-fx-background-color: linear-gradient(to bottom, black 0%%, black %6.2f%%, grey %6.2f%%, black)", 50.0, 50.0));
    }
  }

  private int row = 1;

  private void addSlider(final DoubleProperty property, final String format, String description, double min, double max, final double increment, String longDescription) {
    optionGridPane.add(new Label(description) {{
      setStyle("-fx-font-size: 16px");
    }}, 1, row);
    optionGridPane.add(new Slider(min, max, property.get()) {{
      setStyle("-fx-font-size: 16px");
      valueProperty().bindBidirectional(property);
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
}
