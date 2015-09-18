package com.fsck.k9.fragment.comparator;

import java.util.Comparator;

/**
 * Reverses the result of a {@link Comparator}.
 *
 * @param <T>
 */
public class ReverseComparator<T> implements Comparator<T> {
    private Comparator<T> mDelegate;

    /**
     * @param delegate Never {@code null}.
     */
    public ReverseComparator(final Comparator<T> delegate) {
        mDelegate = delegate;
    }

    @Override
    public int compare(final T object1, final T object2) {
        // arg1 & 2 are mixed up, this is done on purpose
        return mDelegate.compare(object2, object1);
    }
}
