package hs.javafx.demo;

import hs.javafx.carousel.Carousel;

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
 * AbstractCarouselSkin provides the basic functionality for creating TreeView based
 * skins that position their Cells with a PerspectiveTransform.  It provides relevant
 * properties, keeps track of active Cells and the Carousel's current position.  The
 * actual placement of the Cells is controlled by subclasses of AbstractCarouselSkin,
 * which provide further (specialized) properties to control their appearance.
 *
 * TreeCells are used for the Cells that make up a Carousel.  Additional data is
 * associated with these Cells by use of LayoutItem classes.  This was done to prevent
 * having each Skin using a different Cell-class, which would make switching Skins
 * cumbersome (as a new CellFactory would need to be provided as well).
 *
 * Skins can provide methods that are called during layout to allow for further
 * customization of the look of the Carousel.  For example, RayCarouselSkin will allow
 * the user to apply additional rotations, fade outs, etc. to Cells before the final
 * PerspectiveTransform is created.
 *
 * Clipping of Reflections:
 *
 * Much effort was put to make Reflections look as realistic as possible.  Reflections
 * of overlapping items will blend into each other if not clipped due to their partially
 * transparent nature.  Clipping resolves this but adds quite a burden to the system
 * (and also on the design of the Skin).  When Cells are known to never overlap (due to
 * using a low enough density) the clipping can be disabled altogether without a negative
 * impact to the quality.
 *
 * Properties
 * ----------
 * The AbstractCarouselSkin and its subclasses provide properties that affect only one
 * aspect of the Carousel rendering at a time to reduce surprise interactions between
 * properties to a minimum.
 *
 * Still this can be somewhat counter intuitive.  For example, the carousel restricts
 * cells to a maximum width and height and will always try to draw cells at those
 * sizes regardless of any other settings.  This can be somewhat surprising when for
 * example adjusting the view distance -- one would expect the cells (and width of the
 * carousel) to become smaller as the distance increases, however, it only adjusts how
 * deep or squashed the carousel appears (a telelens kind of effect).
 *
 * Assumptions on Cells provided by Cell Factories
 * -----------------------------------------------
 * The TreeCells have some limitations.  Because a PerspectiveTransform always needs
 * to be applied, the effect property is reserved for use by the CarouselSkin.  Also,
 * TreeCells are scaled to fit the maxCellWidth and maxCellHeight restrictions of the
 * Carousel, regardless of their actual size.  Finally, properties that change the
 * Cell's position (like translation and scaling) may not work as expected.  Scaling
 * specifically is automatically accounted for in the final PerspectiveTransform.
 *
 * Depending on the used Skin, certain properties of cells will get overwritten.
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
        images.add(new Image(new FileInputStream(file), 800, 600, true, true));
        if(fileCount++ > 50) {
          break;
        }
      }
    }

    BorderPane borderPane = new BorderPane();

    final Carousel<ImageHandle> carousel = new Carousel<>();

    carousel.setMinWidth(500);
    carousel.setMinHeight(300);
    carousel.setMinWidth(1200);
    carousel.setMinHeight(500);

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
              setGraphic(image);
            }
            else {
              setGraphic(null);
            }
          }
        };

        carouselCell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

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

    ControlPanel controlPanel = new ControlPanel(carousel);

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