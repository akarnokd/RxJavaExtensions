package hu.akarnokd.rxjava2.debug;

/**
 * Remembers the previous hooks overridden by the debug
 * function and allows restoring them via the {@link #restore()}
 * method call.
 * @since 0.17.4
 */
public interface SavedHooks {

    /**
     * Restore the previous set of hooks.
     */
    void restore();
}
