package hs.javafx.control;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import com.sun.javafx.scene.control.behavior.TreeViewBehavior;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;

public class AbstractTreeViewSkin<T> extends BehaviorSkinBase<TreeView<T>, TreeViewBehavior<T>>  {
  private final CellPool<TreeCell<T>> cellPool;

  public AbstractTreeViewSkin(TreeView<T> treeView) {
    super(treeView, new TreeViewBehavior<T>(treeView));

    this.cellPool = new SimpleCellPool<>(treeView, new Callback<TreeView<T>, TreeCell<T>>() {
      @Override
      public TreeCell<T> call(TreeView<T> treeView) {
        return createCell();
      }
    });

    /*
     * TODO Required as otherwise we get NPE's, probably needed in the future anyway.
     */

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
      @Override public Integer call(Integer anchor) {
        return anchor;
      }
    });
    getBehavior().setOnScrollPageUp(new Callback<Integer, Integer>() {
      @Override public Integer call(Integer anchor) {
        return anchor;
      }
    });
    getBehavior().setOnSelectPreviousRow(new Runnable() {
      @Override public void run() { }
    });
    getBehavior().setOnSelectNextRow(new Runnable() {
      @Override public void run() { }
    });
  }

  private static <T> TreeItem<T> findLastExpandedLeaf(TreeItem<T> item) {
    if(!item.isExpanded()) {
      return item;
    }

    ObservableList<TreeItem<T>> children = item.getChildren();

    if(children.isEmpty()) {
      return item;
    }

    return findLastExpandedLeaf(children.get(children.size() - 1));
  }

  protected TreeItem<T> previous(TreeItem<T> item) {
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

    return findLastExpandedLeaf(parent.getChildren().get(index - 1));
  }

  @SuppressWarnings("static-method")
  protected TreeItem<T> next(TreeItem<T> item) {
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

  protected int calculateIndex(TreeItem<T> item) {
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

  protected CellPool<TreeCell<T>> getCellPool() {
    return cellPool;
  }

  public TreeCell<T> createCell() {
    TreeCell<T> cell = getSkinnable().getCellFactory() == null ? createDefaultCellImpl() : getSkinnable().getCellFactory().call(getSkinnable());

    cell.updateTreeView(getSkinnable());

    return cell;
  }

  // TODO copied
  private TreeCell<T> createDefaultCellImpl() {
    return new TreeCell<T>() {
      private HBox hbox;

      @Override
      public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if(item == null || empty) {
          hbox = null;
          setText(null);
          setGraphic(null);
        }
        else {
          // update the graphic if one is set in the TreeItem
          TreeItem<?> treeItem = getTreeItem();
          if(treeItem != null && treeItem.getGraphic() != null) {
            if(item instanceof Node) {
              setText(null);

              // the item is a Node, and the graphic exists, so
              // we must insert both into an HBox and present that
              // to the user (see RT-15910)
              if(hbox == null) {
                hbox = new HBox(3);
              }
              hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item);
              setGraphic(hbox);
            }
            else {
              hbox = null;
              setText(item.toString());
              setGraphic(treeItem.getGraphic());
            }
          }
          else {
            hbox = null;
            if(item instanceof Node) {
              setText(null);
              setGraphic((Node)item);
            }
            else {
              setText(item.toString());
              setGraphic(null);
            }
          }
        }
      }
    };
  }
}
