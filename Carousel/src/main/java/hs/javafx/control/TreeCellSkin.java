package hs.javafx.control;

import javafx.geometry.Insets;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

// WORKAROUND for RT-28390: NPE in TreeCellSkin when TreeCell disclosureNode is set to null
public class TreeCellSkin extends com.sun.javafx.scene.control.skin.TreeCellSkin {

  public TreeCellSkin(TreeCell<?> treeCell) {
    super(treeCell);
  }

  @Override
  protected double computePrefWidth(double height) {
    double labelWidth = super.computePrefWidth(height);

    final Insets padding = getSkinnable().getInsets();
    double pw = padding.getLeft() + padding.getRight();

    TreeView tree = getSkinnable().getTreeView();
    if (tree == null) return pw;

    return labelWidth;
  }

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    layoutLabelInArea(x, y, w, h);
  }
}

