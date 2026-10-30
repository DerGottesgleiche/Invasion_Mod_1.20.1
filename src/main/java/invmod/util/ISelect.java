package invmod.util;

public interface ISelect<T> {
    T selectNext();

    void reset();
}
