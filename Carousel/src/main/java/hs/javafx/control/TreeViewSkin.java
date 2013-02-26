package hs.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleableIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.TreeViewBehavior;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;

/*
 * Paging problem
 * ==============
 * When paging to a new page using pg-up/down it is very hard to determine which cell
 * should be focused on the new page.  When paging down for example, the newly focused
 * cell should be the one that is at the bottom of the page, while the cell that
 * previously had the focus should be at the top of the page.
 *
 * The problem occurs because to know which cell will end up at the bottom of the page
 * is unknown due to cell heights not being known for the newly visible cells.  If
 * their heights vary and are not known immediately (due to background loading of
 * data) the problem in fact is not solvable.
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
 *    navigate with pg-up/down.
 */


public class TreeViewSkin<T> extends BehaviorSkinBase<TreeView<T>, TreeViewBehavior<T>>  {
  private final Set<TreeCell<T>> usedCells = new HashSet<>();
  private final List<TreeCell<T>> reusableCells = new ArrayList<>();

  private final IntegerProperty columns = new StyleableIntegerProperty(1) {
    @Override
    public CssMetaData<? extends Node, Number> getCssMetaData() {
      return StyleableProperties.INDENT;
    }

    @Override
    public Object getBean() {
      return TreeViewSkin.this;
    }

    @Override
    public String getName() {
      return "colums";
    }
  };

  public final IntegerProperty columnsProperty() { return columns; }
  public final int getColumns() { return columns.get(); }
  public final void setColumns(int columns) { this.columns.set(columns); }

  private int maximumCells = 100;

  private TreeCell<T> firstFullyVisibleCell;
  private TreeCell<T> lastFullyVisibleCell;
  private double targetCellOffset = 0.5;

  public TreeViewSkin(TreeView<T> treeView) {
    super(treeView, new TreeViewBehavior<T>(treeView));

    getSkinnable().addEventHandler(ScrollToEvent.SCROLL_TO_TOP_INDEX, new EventHandler<ScrollToEvent<Integer>>() {
      @Override
      public void handle(ScrollToEvent<Integer> event) {
        targetCellOffset = 0.5;
      }
    });

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
          TreeCell<T> treeCell = getTreeCell(current);
          int index = calculateIndex(current);

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
  }


  private double computeTargetCellOffset(TreeCell<T> cell) {
    double cellCenter = cell.getLayoutY() - getSkinnable().getInsets().getTop() + cell.getHeight() / 2;

    System.out.println(">>> layoutY = " + cell.getLayoutY() + "; cell.height = " + cell.getHeight() + "; tree.height = " + getSkinnable().getHeight() + "; insets = " + getSkinnable().getInsets());

    return cellCenter / (getSkinnable().getHeight() - getSkinnable().getInsets().getTop() - getSkinnable().getInsets().getBottom());
  }

  private final Map<TreeItem<T>, TreeCell<T>> treeCells = new HashMap<>();

  protected TreeCell<T> getTreeCell(TreeItem<T> item) {
    TreeCell<T> cell = treeCells.get(item);

    if(cell == null) {
      if(reusableCells.isEmpty()) {
        cell = createCell();
      }
      else {
        cell = reusableCells.remove(reusableCells.size() - 1);
      }

      cell.updateTreeItem(item);
      treeCells.put(item, cell);
    }

    usedCells.add(cell);

    return cell;
  }

  private static <T> TreeItem<T> lastVisibleLeaf(TreeItem<T> item) {
    if(!item.isExpanded()) {
      return item;
    }

    ObservableList<TreeItem<T>> children = item.getChildren();

    if(children.isEmpty()) {
      return item;
    }

    return lastVisibleLeaf(children.get(children.size() - 1));
  }

  private TreeItem<T> previous(TreeItem<T> item) {
    if(item == null) {
      return null;
    }

    TreeItem<T> parent = item.getParent();

    if(parent == null) {
      return null;
    }

    int index = parent.getChildren().indexOf(item);

    if(index == 0) {
      if(parent.getParent() == null && !getSkinnable().isShowRoot()) {
        return null;
      }

      return parent;
    }

    return lastVisibleLeaf(parent.getChildren().get(index - 1));
  }

  private static <T> TreeItem<T> next(TreeItem<T> item) {
    if(item.isExpanded() && !item.getChildren().isEmpty()) {
      return item.getChildren().get(0);
    }

    TreeItem<T> current = item;

    for(;;) {
      TreeItem<T> parent = current.getParent();

      if(parent == null) {
        return null;
      }

      ObservableList<TreeItem<T>> children = parent.getChildren();
      int index = children.indexOf(current) + 1;

      if(index < children.size()) {
        return children.get(index);
      }

      current = parent;
    }
  }

  private int calculateIndex(TreeItem<T> item) {
    TreeItem<T> current = item;
    int index = 0;

    while((current = previous(current)) != null) {
      index++;
    }

    return index;
  }

  @Override
  protected double computePrefWidth(double height) {
    return 20;
  }

  @Override
  protected double computePrefHeight(double width) {
    return 20;
  }

  protected void markAllCellsUnused() {
    usedCells.clear();
  }

  protected void discardUnusedCells() {
    Iterator<TreeCell<T>> iterator = treeCells.values().iterator();

    while(iterator.hasNext()) {
      TreeCell<T> cell = iterator.next();

      if(!usedCells.contains(cell)) {
        iterator.remove();
        cell.updateIndex(-1);  // TODO do more here to empty cell, updateItem, updateTreeItem, etc.
        reusableCells.add(cell);
      }
    }

    if(reusableCells.size() >= maximumCells) {
      reusableCells.subList(maximumCells, reusableCells.size()).clear();
    }
  }

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    markAllCellsUnused();
    getChildren().clear();

    getSkinnable().setClip(new Rectangle(x, y, w, h));

    TreeItem<T> focusedItem = getSkinnable().getFocusModel().getFocusedItem();

    if(focusedItem == null) {
      focusedItem = getSkinnable().getRoot();
    }

    TreeCell<T> targetTreeCell = getTreeCell(focusedItem);

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
    int column = calculateIndex(firstItem) % getColumns();

    /*
     * Move forward until last item on this row so as to calculate the height of the row correctly
     */

    while(column < getColumns() - 1) {
      TreeItem<T> next = next(firstItem);

      if(next == null) {
        break;
      }

      firstItem = next;
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

      TreeCell<T> treeCell = getTreeCell(firstItem);

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

    int index = calculateIndex(firstItem);

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
      TreeCell<T> cell = getTreeCell(firstItem);

      if(!getChildren().contains(cell)) {
        getChildren().add(cell);
      }

      cell.updateIndex(index++);
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

    discardUnusedCells();
  }

  public TreeCell<T> createCell() {
    TreeCell<T> cell;
    if(getSkinnable().getCellFactory() != null) {
      cell = getSkinnable().getCellFactory().call(getSkinnable());
    }
    else {
      cell = createDefaultCellImpl();
    }

    cell.updateTreeView(getSkinnable());

    return cell;
  }

  private TreeCell<T> createDefaultCellImpl() {
    return new TreeCell<T>() {
        private HBox hbox;

        @Override public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                hbox = null;
                setText(null);
                setGraphic(null);
            } else {
                // update the graphic if one is set in the TreeItem
                TreeItem<?> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getGraphic() != null) {
                    if (item instanceof Node) {
                        setText(null);

                        // the item is a Node, and the graphic exists, so
                        // we must insert both into an HBox and present that
                        // to the user (see RT-15910)
                        if (hbox == null) {
                            hbox = new HBox(3);
                        }
                        hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item);
                        setGraphic(hbox);
                    } else {
                        hbox = null;
                        setText(item.toString());
                        setGraphic(treeItem.getGraphic());
                    }
                } else {
                    hbox = null;
                    if (item instanceof Node) {
                        setText(null);
                        setGraphic((Node)item);
                    } else {
                        setText(item.toString());
                        setGraphic(null);
                    }
                }
            }
        }
    };
  }


  /** @treatAsPrivate */
  private static class StyleableProperties {

    @SuppressWarnings("rawtypes")
    private static final CssMetaData<TreeView, Number> INDENT =
      new CssMetaData<TreeView, Number>("-fx-columns", SizeConverter.getInstance(), 1) {
        @Override public boolean isSettable(TreeView n) {
          IntegerProperty p = ((TreeViewSkin) n.getSkin()).columnsProperty();
          return p == null || !p.isBound();
        }

        @Override public StyleableIntegerProperty getStyleableProperty(TreeView n) {
          final TreeViewSkin skin = (TreeViewSkin) n.getSkin();
          return (StyleableIntegerProperty)skin.columnsProperty();
        }
      };

    private static final List<CssMetaData<? extends Node, ?>> STYLEABLES;

    static {
      final List<CssMetaData<? extends Node, ?>> styleables = new ArrayList<>(SkinBase.getClassCssMetaData());

      Collections.addAll(styleables,
        INDENT
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
