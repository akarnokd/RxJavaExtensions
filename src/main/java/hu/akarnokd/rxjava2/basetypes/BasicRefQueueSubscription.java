package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.internal.fuseable.QueueSubscription;

/**
 * Base class extending AtomicInteger (wip or request accounting) and QueueSubscription (fusion).
 *
 * @param <T> the value type
 * @param <R> the reference type
 */
public abstract class BasicRefQueueSubscription<T, R> extends AtomicReference<R> implements QueueSubscription<T> {


    private static final long serialVersionUID = -6671519529404341862L;

    @Override
    public final boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final boolean offer(T v1, T v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public int requestFusion(int mode) {
        return mode & ASYNC;
    }

    @Override
    public T poll() throws Exception {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void clear() {
        // no-op
    }

    @Override
    public void request(long n) {
        // ignored
    }
}