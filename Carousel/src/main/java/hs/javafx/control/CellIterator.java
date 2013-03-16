package hs.javafx.control;

import java.util.Iterator;

import javafx.scene.control.TreeCell;
import javafx.scene.shape.Shape;

public interface CellIterator extends Iterator<TreeCell<?>> {
  Shape getClip();
}
