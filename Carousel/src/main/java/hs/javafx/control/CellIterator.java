package hs.javafx.control;

import java.util.Iterator;

import javafx.scene.control.TreeCell;
import javafx.scene.shape.Shape;

public interface CellIterator<T> extends Iterator<TreeCell<T>> {
  Shape getClip();
}
