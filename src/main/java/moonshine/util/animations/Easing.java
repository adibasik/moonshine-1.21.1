package moonshine.util.animations;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}