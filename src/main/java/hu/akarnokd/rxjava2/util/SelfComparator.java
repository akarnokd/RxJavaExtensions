package hu.akarnokd.rxjava2.util;

import java.util.Comparator;

/**
 * Comparator that compares Comparables.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public enum SelfComparator implements Comparator<Comparable> {
    INSTANCE;
    
    
    public static <T extends Comparable<? super T>> Comparator<T> instance() {
        return (Comparator)INSTANCE;
    }
    
    @Override
    public int compare(Comparable o1, Comparable o2) {
        return o1.compareTo(o2);
    }
}
