package hu.akarnokd.rxjava2.util;

/**
 * Utility class to throw arbitrary Throwables.
 */
public final class SneakyThrows {

    private SneakyThrows() {
        throw new IllegalStateException("No instances!");
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> E justThrow(Throwable error) throws E {
        throw (E)error;
    }

}
