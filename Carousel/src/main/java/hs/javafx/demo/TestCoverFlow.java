package hs.javafx.demo;

import hs.javafx.carousel.CarouselSkin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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
 *
 * Implementation details
 * ----------------------
 * CarouselSkin delegates much of its functionality to Layouts.  A Layout determines
 * how many cells are needed and where they get placed.  This provides a location for
 * Properties and customization Methods that control the look of the Layout -- these
 * are often very specific and thus are not part of CarouselSkin.
 *
 * When layoutChildren() is called on the Skin, the Layout is asked to provide a
 * CellIterator -- this Iterator provides a number of Cells that need to be positioned.
 * The placement of cells can be dependent on earlier cells that have been positioned and
 * the Iterator has state associated with it to keep track of this.  For example, when
 * cells of different sizes should be positioned so they are touching each other, there is
 * a need to know where previous cells were positioned.  Furthermore, the number of cells
 * the iterator returns differs by implementation.  Some return a constant number of cells
 * (when cells are equally spaced for example) or a number dependent on the sizes of the
 * cells involved.
 *
 * Some layouts currently also provide ways to override certain behaviour by subclassing
 * them, specifically RayLayout.  This is work in progress.
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
 * The Carousel and its Layouts provide properties that affect only one aspect of the
 * Carousel rendering at the same time so as to reduce surprises.  Still this can be
 * somewhat counter intuitive.  For example, the carousel restricts cells to a maximum
 * width and height and will always try to draw cells at those sizes regardless of any
 * other settings.
 *
 * This can be somewhat surprising when for example adjusting the view distance -- one
 * would expect the cells (and width of the carousel) to become smaller as the distance
 * increases, however, it only adjusts how deep or squashed the carousel appears (a tele
 * lens kind of effect).
 *
 * Assumptions on Cells provided by Cell Factories
 * -----------------------------------------------
 * Depending on the Layout chosen, certain properties of cells will get overwritten.
 *
 * These can include:
 * - Effect property; used to do scaling and reflections
 * - Clip property; used to clip reflections
 * - Opacity property; used for fade in/out
 *
 * Furthermore, Cells are always rendered at their preferred width and height, the
 * PerspectiveTransform takes care of the scaling.  This means that Borders on very big
 * cells will not be as thick as the same Borders on a very small cell.  The Cell
 * supplier should be aware of this and adjust their cells preferred width/height
 * accordingly if desired.
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

    ControlPanel controlPanel = new ControlPanel((CarouselSkin<?>)carousel.getSkin());

    borderPane.setBottom(controlPanel);
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
}
