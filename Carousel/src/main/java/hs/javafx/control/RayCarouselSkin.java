package hs.javafx.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.effect.Effect;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public class RayCarouselSkin<T> extends AbstractCarouselSkin<T> {

  public RayCarouselSkin(final Carousel<T> carousel) {
    super(carousel);

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        requestLayout();
      }
    };

    carousel.cellAlignmentProperty().addListener(invalidationListener);
    carousel.reflectionEnabledProperty().addListener(invalidationListener);
    carousel.fieldOfViewRatioProperty().addListener(invalidationListener);
    carousel.radiusRatioProperty().addListener(invalidationListener);
    carousel.viewDistanceRatioProperty().addListener(invalidationListener);
  }

  private class CellConfigurator {
    private final CarouselCell<T> cell;
    private final double index;
    private final double maxCellWidth;
    private final double maxCellHeight;

    private Effect reflection;
    private double reflectionTop;
    private double reflectionSpace;

    private Point3D ul;
    private Point3D ur;
    private Point3D ll;
    private Point3D lr;
    private Point3D ulReflection;
    private Point3D urReflection;
    private Point2D ulReflection2d;
    private Point2D urReflection2d;

    private PerspectiveTransform perspectiveTransform;

    public CellConfigurator(CarouselCell<T> cell, double index, double maxCellWidth, double maxCellHeight) {
      this.cell = cell;
      this.index = index;
      this.maxCellWidth = maxCellWidth;
      this.maxCellHeight = maxCellHeight;
    }

    public void addReflection() {
      double reflectionMaxHeight = 50;

      double w = cell.prefWidth(50);
      double h = cell.prefHeight(50);

      double verticalAlignment = getSkinnable().getCellAlignment();

      double topOfCell = (maxCellHeight - h) * verticalAlignment;
      double reflectionTop = 2 * (maxCellHeight - topOfCell - h);
      double reflectionTopOpacity = 0.5 - 0.5 / reflectionMaxHeight * reflectionTop / 2;
      double reflectionBottomOpacity = 0;
      double reflectionPortion = (reflectionMaxHeight - reflectionTop / 2) / h;

      if(reflectionPortion < 0 || reflectionTopOpacity < 0) {
        reflectionTopOpacity = 0;
        reflectionPortion = 0;
      }
      if(reflectionPortion > 1) {
        reflectionBottomOpacity = 0.5 - 0.5 / reflectionPortion;
        reflectionPortion = 1;
      }

      double reflectionHeight = h * reflectionPortion;

      //System.err.println(">>> cell : " + cell.getSkin());
//      Insets insets = ((StackPane)cell
//          .getSkin()
//          .getNode())
//          .getInsets();
//      double insetHeight = insets.getBottom();

//      reflection = new Blend(
//        BlendMode.OVERLAY,
//        new Reflection(reflectionTop - insetHeight - 1, reflectionPortion, reflectionTopOpacity, reflectionBottomOpacity),
//        new ColorInput(0.0, reflectionTop + h, w, reflectionHeight, Color.WHITE)
//      );

      if(reflectionPortion > 0) {
        this.reflection = new Reflection(reflectionTop, reflectionPortion, reflectionTopOpacity, reflectionBottomOpacity);

        this.reflectionTop = reflectionTop;
        this.reflectionSpace = reflectionHeight + reflectionTop;
      }
    }

    public void calculateCarouselCoordinates() {
      double visibleAngle = Math.PI * 0.75;
      double visibleCellsCount = getVisibleCellsCount();
      double angleOnCarousel = visibleAngle / visibleCellsCount * index + Math.PI * 0.5;

      double carouselRadius = getSkinnable().getWidth() * getSkinnable().getRadiusRatio();
      double halfCellWidth = cell.prefWidth(50) * 0.5;
      double h = cell.prefHeight(50);

      double uy = -maxCellHeight * 0.5 + (maxCellHeight - h) * getSkinnable().getCellAlignment();
      double ly = uy + h + reflectionSpace;

      double cos = Math.cos(angleOnCarousel);
      double sin = -Math.sin(angleOnCarousel);

      ul = new Point3D((carouselRadius + halfCellWidth) * cos, uy, (carouselRadius + halfCellWidth) * sin);
      ur = new Point3D((carouselRadius - halfCellWidth) * cos, uy, (carouselRadius - halfCellWidth) * sin);
      ll = new Point3D((carouselRadius + halfCellWidth) * cos, ly, (carouselRadius + halfCellWidth) * sin);
      lr = new Point3D((carouselRadius - halfCellWidth) * cos, ly, (carouselRadius - halfCellWidth) * sin);

      if(reflection != null) {
        ulReflection = new Point3D((carouselRadius + halfCellWidth) * cos, uy + h + reflectionTop, (carouselRadius + halfCellWidth) * sin);
        urReflection = new Point3D((carouselRadius - halfCellWidth) * cos, uy + h + reflectionTop, (carouselRadius - halfCellWidth) * sin);
      }

    //Equivalent code:
//    Point3D ul = new Point3D(carouselRadius / scaleFactor + w / 2, -h / 2, 0);
//    Point3D ur = new Point3D(carouselRadius / scaleFactor - w / 2, -h / 2, 0);
//    Point3D ll = new Point3D(carouselRadius / scaleFactor + w / 2, h / 2, 0);
//    Point3D lr = new Point3D(carouselRadius / scaleFactor - w / 2, h / 2, 0);
//
//    Point3D carouselAxis = new Point3D(0, 0, 0);
//
//    ul = rotateY(ul, carouselAxis, angleOnCarousel);
//    ur = rotateY(ur, carouselAxis, angleOnCarousel);
//    ll = rotateY(ll, carouselAxis, angleOnCarousel);
//    lr = rotateY(lr, carouselAxis, angleOnCarousel);
    }

    /**
     * Rotates the Cell towards the Viewer when it is close to the center.  Also mirrors
     * the cells after they passed the center to keep the Cells correctly visible for the
     * viewer.
     */
    public void applyViewRotation() {
      if(index < 3) {
        double angle = index > -3 ? Math.PI / 2 * -index / 3 + Math.PI / 2 : Math.PI;

        Point3D axis = new Point3D((ul.getX() + ur.getX()) / 2, 0, (ul.getZ() + ur.getZ()) / 2);

        ul = rotateY(ul, axis, angle);
        ur = rotateY(ur, axis, angle);
        ll = rotateY(ll, axis, angle);
        lr = rotateY(lr, axis, angle);

        if(reflection != null) {
          ulReflection = rotateY(ulReflection, axis, angle);
          urReflection = rotateY(urReflection, axis, angle);
        }
      }
    }

    public Shape getReflectionClip() {
      if(reflection == null) {
        return null;
      }

      return new Polygon(
        ulReflection2d.getX(), ulReflection2d.getY(),
        urReflection2d.getX(), urReflection2d.getY(),
        perspectiveTransform.getLrx(), perspectiveTransform.getLry(),
        perspectiveTransform.getLlx(), perspectiveTransform.getLly()
      );
    }

    public PerspectiveTransform build() {
      double w = cell.prefWidth(50);
      double h = cell.prefHeight(50);

      double viewDistance = getWidth() * getSkinnable().getViewDistanceRatio() + getWidth() * getSkinnable().getRadiusRatio();
      double fov = getSkinnable().getFieldOfViewRatio() * getSkinnable().getWidth();
      double cw = w / 2;
      double ch = h / 2;

      Point2D ul2d = project(ul, viewDistance, fov, cw, ch);
      Point2D ur2d = project(ur, viewDistance, fov, cw, ch);
      Point2D ll2d = project(ll, viewDistance, fov, cw, ch);
      Point2D lr2d = project(lr, viewDistance, fov, cw, ch);

      perspectiveTransform = new PerspectiveTransform(ul2d.getX(), ul2d.getY(), ur2d.getX(), ur2d.getY(), lr2d.getX(), lr2d.getY(), ll2d.getX(), ll2d.getY());

      if(reflection != null) {
        ulReflection2d = project(ulReflection, viewDistance, fov, cw, ch);
        urReflection2d = project(urReflection, viewDistance, fov, cw, ch);

        perspectiveTransform.setInput(reflection);
      }

      return perspectiveTransform;
    }
  }

  @Override
  public Shape layoutCell(CarouselCell<T> cell, double index, double maxCellWidth, double maxCellHeight) {
    CellConfigurator configurator = new CellConfigurator(cell, index, maxCellWidth, maxCellHeight);

    if(getSkinnable().getReflectionEnabled()) {
      configurator.addReflection();
    }
    configurator.calculateCarouselCoordinates();
    configurator.applyViewRotation();

    PerspectiveTransform perspectiveTransform = configurator.build();

    cell.setEffect(perspectiveTransform);

    return configurator.getReflectionClip();
  }

  @Override
  public Point2D positionCell(CarouselCell<T> cell, double index, double maxCellWidth, double maxCellHeight) {
    double visibleCellsCount = getVisibleCellsCount();

    double position = Math.sin(index / visibleCellsCount * Math.PI) * (getWidth() / 3);

    return new Point2D(0, 0);
  }

  private static Point2D project(Point3D p, double viewDistance, double fov, double cw, double ch) {
    return new Point2D(p.getX() * fov / (p.getZ() + viewDistance) + cw, p.getY() * fov / (p.getZ() + viewDistance) + ch);
  }

  private static Point3D rotateY(Point3D p, Point3D axis, double radians) {
    Point3D input = new Point3D(p.getX() - axis.getX(), p.getY() - axis.getY(), p.getZ() - axis.getZ());

    return new Point3D(
      input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + axis.getX(),
      input.getY() + axis.getY(),
      input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + axis.getZ()
    );
  }
}
