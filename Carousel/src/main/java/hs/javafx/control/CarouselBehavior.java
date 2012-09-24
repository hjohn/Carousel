package hs.javafx.control;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.behavior.OrientedKeyBinding;

public class CarouselBehavior<T> extends BehaviorBase<Carousel<T>> {

  public CarouselBehavior(Carousel<T> carousel) {
    super(carousel);
  }

  @Override
  protected void callAction(String action) {
    if("FocusPreviousRow".equals(action)) {
      focusPreviousRow();
    }
    else if("FocusNextRow".equals(action)) {
      focusNextRow();
    }
    System.out.println("Action: " + action);

    super.callAction(action);
  }

  private void focusPreviousRow() {
    FocusModel<T> focusModel = getControl().getFocusModel();

    if(focusModel == null) {
      return;
    }

    focusModel.focusPrevious();
  }

  private void focusNextRow() {
    FocusModel<T> focusModel = getControl().getFocusModel();

    if(focusModel == null) {
      return;
    }

    focusModel.focusNext();
  }

  protected static List<KeyBinding> CAROUSEL_BINDINGS = new ArrayList<>();

  @Override
  protected List<KeyBinding> createKeyBindings() {
    return CAROUSEL_BINDINGS;
  }

  static {
    CAROUSEL_BINDINGS.add(new KeyBinding(KeyCode.TAB, "TraverseNext"));
    CAROUSEL_BINDINGS.add(new KeyBinding(KeyCode.TAB, "TraversePrevious").shift());

    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.LEFT, "TraverseLeft").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_LEFT, "TraverseLeft").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.RIGHT, "TraverseRight").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_RIGHT, "TraverseRight").vertical());

    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.LEFT, "FocusPreviousRow"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_LEFT, "FocusPreviousRow"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.RIGHT, "FocusNextRow"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_RIGHT, "FocusNextRow"));

    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.UP, "FocusPreviousRow").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_UP, "FocusPreviousRow").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.DOWN, "FocusNextRow").vertical());
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_DOWN, "FocusNextRow").vertical());

    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.UP, "TraverseUp"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_UP, "TraverseUp"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.DOWN, "TraverseDown"));
    CAROUSEL_BINDINGS.add(new CarouselKeyBinding(KeyCode.KP_DOWN, "TraverseDown"));
  }

  private static class CarouselKeyBinding extends OrientedKeyBinding {
    public CarouselKeyBinding(KeyCode paramKeyCode, String paramString) {
      super(paramKeyCode, paramString);
    }

    public CarouselKeyBinding(KeyCode paramKeyCode, EventType<KeyEvent> paramEventType, String paramString) {
      super(paramKeyCode, paramEventType, paramString);
    }

    @Override
    public boolean getVertical(Control control) {
      return ((Carousel<?>)control).getOrientation() == Orientation.VERTICAL;
    }
  }
}
