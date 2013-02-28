package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.SizeConverter;

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

/*
 * Paging problem
 * ==============
 * When paging to a new page using pg-up/down it is very hard to determine which cell
 * should be focused on the new page.  When paging down for example, the newly focused
 * cell should be the one that is at the bottom of the page, while the cell that
 * previously had the focus should be at the top of the page.
 *
 * The problem occurs because which cell will end up at the bottom of the page is
 * unknown due to cell heights not being known for the newly visible cells.  If their
 * heights vary and are not known immediately (due to background loading of data) the
 * problem is in fact not solvable.
 *
 * There are several situations we can distinguish:
 *
 * 1) Cell heights are final (fixed height or not).  The newly focused cell can be
 *    found by querying the heights of all cells starting from the new top cell to the
 *    first cell found to not fit in the view.
 *
 * 2) Cell heights are not final; the final value can only be determined after fully
 *    loading the cell.  This problem is not solvable.  A possible solution is to
 *    change the pg-down/up functionality for this case.  Possibilities could be:
 *
 *    a) Scroll a fixed, selectable number of cells (like 5).
 *
 *    b) Scroll a percentage of the currently visible cells, in the hope that the next
 *       page will roughly have the same cell height distribution.
 *
 *    c) Move the current focused cell to the top, but instead of focusing the new
 *       bottom cell leave the top one focused until the user pages again.  This means
 *       "paging" effectively requires two pg-down/up pressed, but it gives a bit of
 *       time to let the new page load.
 *
 *    Of these options only the two-step paging potentially can avoid skipping over
 *    cells.  The other methods can avoid it as well in most practical cases, where
 *    practical means that the control shows a reasonable number of visible cells to
 *    make a View control a practical control for displaying them (a View control that
 *    shows say less than 5 cells in certain situations is not very practical to
 *    navigate with pg-up/down).
 */

public class MultiColumnTreeViewSkin<T> extends AbstractTreeViewSkin<T> {
  private final IntegerProperty columns = new StyleableIntegerProperty(1) {
    @Override
    public CssMetaData<? extends Node, Number> getCssMetaData() {
      return StyleableProperties.COLUMNS;
    }

    @Override
    public Object getBean() {
      return this;
    }

    @Override
    public String getName() {
      return "colums";
    }
  };
  public final IntegerProperty columnsProperty() { return columns; }
  public final int getColumns() { return columns.get(); }
  public final void setColumns(int columns) { this.columns.set(columns); }

  private TreeCell<T> firstFullyVisibleCell;
  private TreeCell<T> lastFullyVisibleCell;
  private double targetCellOffset = 0.5;

  public MultiColumnTreeViewSkin(TreeView<T> treeView) {
    super(treeView);

    getBehavior().setOnFocusPreviousRow(new Runnable() {
      @Override public void run() { }
    });
    getBehavior().setOnFocusNextRow(new Runnable() {
        @Override public void run() { }
    });
    getBehavior().setOnMoveToFirstCell(new Runnable() {
        @Override public void run() { }
    });
    getBehavior().setOnMoveToLastCell(new Runnable() {
        @Override public void run() { }
    });
    getBehavior().setOnScrollPageDown(new Callback<Integer, Integer>() {
        @Override
        public Integer call(Integer anchor) {
          if(lastFullyVisibleCell.isFocused()) {
            targetCellOffset = 0;
            getSkinnable().requestLayout();
          }

          return lastFullyVisibleCell.getIndex();
        }
    });
    getBehavior().setOnScrollPageUp(new Callback<Integer, Integer>() {
        @Override
        public Integer call(Integer anchor) {
          if(firstFullyVisibleCell.isFocused()) {
            targetCellOffset = 1;
            getSkinnable().requestLayout();
          }

          return firstFullyVisibleCell.getIndex();
        }
    });
    getBehavior().setOnSelectPreviousRow(new Runnable() {
        @Override public void run() { }
    });
    getBehavior().setOnSelectNextRow(new Runnable() {
        @Override public void run() { }
    });

    getSkinnable().getFocusModel().focusedItemProperty().addListener(new ChangeListener<TreeItem<T>>() {
      @Override
      public void changed(ObservableValue<? extends TreeItem<T>> observableValue, TreeItem<T> old, TreeItem<T> current) {
        System.out.println(">>> focus changed from " + old + " to " + current);

        if(current != null) {
          int index = calculateIndex(current);
          TreeCell<T> treeCell = getCellPool().getCell(index);

          if(firstFullyVisibleCell != null && index < firstFullyVisibleCell.getIndex()) {
            targetCellOffset = 0;
            getSkinnable().requestLayout();
          }
          else if(lastFullyVisibleCell != null && index > lastFullyVisibleCell.getIndex()) {
            targetCellOffset = 1;
            getSkinnable().requestLayout();
          }
          else {
            targetCellOffset = computeTargetCellOffset(treeCell);
          }

          System.out.println(">>> targetCellOffset = " + targetCellOffset + "; index = " + index + "; lastFullyVisibleCell = " + lastFullyVisibleCell);
        }
      }
    });

    getSkinnable().addEventHandler(ScrollToEvent.SCROLL_TO_TOP_INDEX, new EventHandler<ScrollToEvent<Integer>>() {
      @Override
      public void handle(ScrollToEvent<Integer> event) {
        targetCellOffset = 0.5;
      }
    });
  }

  private double computeTargetCellOffset(TreeCell<T> cell) {
    double cellCenter = cell.getLayoutY() - getSkinnable().getInsets().getTop() + cell.getHeight() / 2;

    System.out.println(">>> layoutY = " + cell.getLayoutY() + "; cell.height = " + cell.getHeight() + "; tree.height = " + getSkinnable().getHeight() + "; insets = " + getSkinnable().getInsets());

    return cellCenter / (getSkinnable().getHeight() - getSkinnable().getInsets().getTop() - getSkinnable().getInsets().getBottom());
  }

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    getCellPool().reset();

    getChildren().clear();

    getSkinnable().setClip(new Rectangle(x, y, w, h));

    TreeItem<T> focusedItem = getSkinnable().getFocusModel().getFocusedItem();

    if(focusedItem == null) {
      focusedItem = getSkinnable().getRoot();
    }

    TreeCell<T> targetTreeCell = getCellPool().getCell(calculateIndex(focusedItem));

    getChildren().add(targetTreeCell);

    double cellHeight = targetTreeCell.prefHeight(-1);   // TODO incorrect, needs to be maxCellHeight for the row
    double startY = snapPosition(h * targetCellOffset - cellHeight / 2);

    if(startY + cellHeight > h) {
      startY = h - cellHeight;
    }
    if(startY < 0) {
      startY = 0;
    }

    TreeItem<T> firstItem = focusedItem;

    System.out.println(">>> Laying out, startY = " + startY + " cellHeight = " + cellHeight + ", focusedItem = " + focusedItem);
    int index = calculateIndex(firstItem);
    int column = index % getColumns();

    /*
     * Move forward until last item on this row so as to calculate the height of the row correctly
     */

    while(column < getColumns() - 1) {
      TreeItem<T> next = next(firstItem);

      if(next == null) {
        break;
      }

      firstItem = next;
      index++;
      column++;
    }

    /*
     * Move backward until last visible item
     */

    double maxColumnHeight = 0;

    while(startY > 0 || column > 0) {
      TreeItem<T> previous = previous(firstItem);

      if(previous == null) {
        break;
      }

      firstItem = previous;
      index--;

      TreeCell<T> treeCell = getCellPool().getCell(index);

      if(!getChildren().contains(treeCell)) {
        getChildren().add(treeCell);
      }

      maxColumnHeight = Math.max(maxColumnHeight, treeCell.prefHeight(-1));

      if(column-- == 0) {
        column = getColumns() - 1;
        startY -= maxColumnHeight;
        maxColumnHeight = 0;
      }
    }

    System.out.println(">>> Laying out " + x + "; " + y + "; " + w + "x" + h + ", startIndex = " + index + ", firstItem = " + firstItem + ", focusedItem = " + focusedItem);

    if(startY > 0) {
      startY = 0;
    }

    /*
     * firstCell = first cell to draw
     * startY = y position of first cell to draw
     */

    double startX = x;
    startY += y;

    firstFullyVisibleCell = null;

    while(firstItem != null && startY < h) {
      TreeCell<T> cell = getCellPool().getCell(index);

      if(!getChildren().contains(cell)) {
        getChildren().add(cell);
      }

      index++;
      double ch = cell.prefHeight(-1);
      cell.resizeRelocate(startX, startY, w / getColumns(), ch);

      if(startY >= 0 && firstFullyVisibleCell == null) {
        firstFullyVisibleCell = cell;
      }
      if(startY - y + ch <= h) {
        lastFullyVisibleCell = cell;
      }

      if(++column == getColumns()) {
        startY += ch;  // TODO incorrect, needs to be maxCellHeight for the row
        column = 0;
        startX = x;
      }
      else {
        startX += w / getColumns();
      }

      firstItem = next(firstItem);
    }

    getCellPool().trim();
  }

  /** @treatAsPrivate */
  private static class StyleableProperties {

    @SuppressWarnings("rawtypes")
    private static final CssMetaData<TreeView, Number> COLUMNS =
      new CssMetaData<TreeView, Number>("-fx-columns", SizeConverter.getInstance(), 1) {
        @Override public boolean isSettable(TreeView n) {
          IntegerProperty p = ((MultiColumnTreeViewSkin) n.getSkin()).columnsProperty();
          return p == null || !p.isBound();
        }

        @Override public StyleableIntegerProperty getStyleableProperty(TreeView n) {
          final MultiColumnTreeViewSkin skin = (MultiColumnTreeViewSkin) n.getSkin();
          return (StyleableIntegerProperty)skin.columnsProperty();
        }
      };

    private static final List<CssMetaData<? extends Node, ?>> STYLEABLES;

    static {
      final List<CssMetaData<? extends Node, ?>> styleables = new ArrayList<>(SkinBase.getClassCssMetaData());

      Collections.addAll(styleables,
        COLUMNS
      );

      STYLEABLES = Collections.unmodifiableList(styleables);
    }
  }

  /**
   * @return The CssMetaData associated with this class, which may include the
   * CssMetaData of its super classes.
   */
  public static List<CssMetaData<? extends Node, ?>> getClassCssMetaData() {
      return StyleableProperties.STYLEABLES;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CssMetaData<? extends Node, ?>> getCssMetaData() {
      return getClassCssMetaData();
  }
}
