package hs.javafx.control;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Coverflow / Image Carousel demonstration
 * ========================================
 *
 * This is work in progress and is still missing key features, like:
 *
 * - Scrollbar(s)
 * - Page down/up navigation
 * - Navigation orientation (up/down must be used currently instead of left/right)
 * - Expansion of tree nodes
 * - Customizable reflections
 * - Customizable animation
 * - Customizable cell fading/scaling
 *
 * Implementation details
 * ----------------------
 * CarouselSkin delegates much of its functionality to Layouts.  A Layout determines
 * how many cells are needed and where they get placed.  The reason for doing this
 * is to provide a location for Properties that control the look of the Layout -- these
 * are often very layout specific and thus are not part of CarouselSkin.  An alternative
 * would have been to create a different Skin for each Layout, however, this makes it
 * hard to switch between different carousel layouts.
 *
 * When layoutChildren() is called on the Skin, the Layout is asked to provide a
 * CellIterator -- this Iterator provides a number of Cells that need to be positioned.
 * The placement of cells can be dependent on earlier cells that have been positioned and
 * the Iterator has state associated with it to keep track of this.  For example, when
 * cells of different sizes should be positioned so they are touching each other, there is
 * need to know where previous cells were positioned.  Furthermore, the number of cells
 * the iterator returns differs by implementation.  Some return a constant number of cells
 * (when cells are equally spaced for example) or a fluctuating number depending on the
 * sizes of the cells involved.
 *
 * The CellIterator is a standard Iterator with one other method that queries the Clip of
 * the last returned cell.  This is used to prevent cells positioned after the current
 * cell to blend with portions of earlier positioned cells -- its main use currently is to
 * avoid reflections from later positioned cells to show through the reflection of earlier
 * positioned cells (which have a higher Z order).
 *
 * Without this clip, other cells (with lower Z order) can show through these transparent
 * portions which can cause a blending of the individual reflections of each cell which is
 * undesirable.  The use of clipping however is optional, as it is only needed when Cells
 * are allowed to partially cover each other.
 *
 * Notes
 * -----
 * The Carousel provides controls that affect only one aspect of the Carousel rendering at
 * the same time so as to reduce surprises.  Still this can be somewhat counter intuitive.
 * For example, the carousel restricts cells to a maximum width and height and will always
 * try to draw cells at those sizes regardless of any other settings.
 *
 * This can be somewhat surprising when for example adjusting the view distance -- one
 * would expect the cells (and width of the carousel) to become smaller as the distance
 * increases, however, it only adjusts how deep or squashed the carousel appears (a tele
 * lens kind of effect).
 *
 * Assumptions
 * -----------
 * 1) Cells donot have an Effect set, this is set by the carousel (a PerspectiveTransform
 *    and optionally a Reflection).
 *
 * 2) Cells donot have a Clip set, this is set by the carousel when reflections need
 *    clipping.
 *
 * 3) Cells are always rendered at their preferred width and height, the
 *    PerspectiveTransform takes care of the scaling.  This means that Borders on very big
 *    cells will not be as thick as the same Borders on a very small cell.  The Cell
 *    supplier should be aware of this and adjust their cells preferred width/height
 *    accordingly if desired.
 */
public class TestCoverFlow extends Application {

  public static void main(String[] args) {
    Application.launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    DirectoryChooser directoryChooser = new DirectoryChooser();

    directoryChooser.setTitle("Choose a directory with images");
    File dir = directoryChooser.showDialog(null);

    List<Image> images = new ArrayList<>();
    int fileCount = 0;

    for(File file : dir.listFiles()) {
      if(file.isFile()) {
        images.add(new Image(new FileInputStream(file)));
        if(fileCount++ > 50) {
          break;
        }
      }
    }

    BorderPane borderPane = new BorderPane();

    final TreeView<ImageHandle> carousel = new TreeView<>();

    carousel.setMinWidth(500);
    carousel.setMinHeight(300);

    TreeItem<ImageHandle> root = new TreeItem<>();

    carousel.setRoot(root);
    carousel.setShowRoot(false);

    for(Image image : images) {
      root.getChildren().add(new TreeItem<>(new ImageHandle(new ImageView(image))));
    }

    carousel.setCellFactory(new Callback<TreeView<ImageHandle>, TreeCell<ImageHandle>>() {
      @Override
      public TreeCell<ImageHandle> call(final TreeView<ImageHandle> carousel) {
        TreeCell<ImageHandle> carouselCell = new TreeCell<ImageHandle>() {
          @Override
          protected void updateItem(ImageHandle item, boolean empty) {
            super.updateItem(item, empty);

            if(!empty) {
              ImageView image = item.getImage();
              image.setPreserveRatio(true);
//              image.fitWidthProperty().bind(carousel.maxCellWidthProperty());
//              image.fitHeightProperty().bind(carousel.maxCellHeightProperty());
              setGraphic(image);
            }
            else {
              setGraphic(null);
            }
          }
        };

        carouselCell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        carouselCell.setDisclosureNode(new Rectangle(0,0,0,0));
        //carouselCell.setEffect(new Reflection());

        return carouselCell;
      }
    });


    Scene scene = new Scene(borderPane);

    scene.getStylesheets().add("Carousel.css");
    stage.setScene(scene);
    borderPane.setTop(carousel);
    stage.setWidth(1280);
    stage.setHeight(720);
    stage.show();

    final CarouselSkin<?> carouselSkin = (CarouselSkin<?>)carousel.getSkin();
    Layout<?> genericLayout = carouselSkin.getLayout();

    if(genericLayout instanceof RayLayout) {
      final RayLayout<?> layout = (RayLayout<?>)genericLayout;

      layout.viewAlignmentProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number old, Number current) {
          double f = carouselSkin.getMaxCellHeight() / carousel.getHeight();
          double c = ((1.0 - f) / 2 + current.doubleValue() * f) * 100;
          carousel.setStyle(String.format("-fx-background-color: linear-gradient(to bottom, black 0%%, black %6.2f%%, grey %6.2f%%, black)", c, c));
        }
      });
    }

    fillOptionGridPane(carouselSkin, genericLayout);

    optionGridPane.setPadding(new Insets(20.0));

    borderPane.setBottom(optionGridPane);

  }

  private static class ImageHandle {
    private final ImageView imageView;

    public ImageHandle(ImageView imageView) {
      this.imageView = imageView;
    }

    public ImageView getImage() {
      return imageView;
    }
  }

  private GridPane optionGridPane = new GridPane();

  public void fillOptionGridPane(final CarouselSkin<?> skin, Layout<?> genericLayout) {
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
      RayLayout<?> layout = (RayLayout<?>) genericLayout;

      addSlider(layout.radiusRatioProperty(), "%4.2f", "Radius Ratio (0.0 - 2.0)", 0.0, 2.0, 0.1, "The radius of the carousel expressed as the fraction of half the view's width");
      addSlider(layout.viewDistanceRatioProperty(), "%4.2f", "View Distance Ratio (0.0 - 4.0)", 0.0, 4.0, 0.1, "The distance of the camera expressed as a fraction of the radius of the carousel");
      addSlider(layout.carouselViewFractionProperty(), "%4.2f", "Carousel View Fraction (0.0 - 1.0)", 0.0, 1.0, 0.1, "The portion of the carousel that is used for displaying cells");
      addSlider(layout.viewAlignmentProperty(), "%4.2f", "View Alignment (0.0 - 1.0)", 0.0, 1.0, 0.1, "The vertical alignment of the camera with respect to the carousel");
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
