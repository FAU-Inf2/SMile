package com.fsck.k9.fragment.comparator;

import java.util.Comparator;
import java.util.List;

/**
 * Chains comparator to find a non-0 result.
 *
 * @param <T>
 */
public class ComparatorChain<T> implements Comparator<T> {
    private List<Comparator<T>> mChain;

    /**
     * @param chain Comparator chain. Never {@code null}.
     */
    public ComparatorChain(final List<Comparator<T>> chain) {
        mChain = chain;
    }

    @Override
    public int compare(T object1, T object2) {
        int result = 0;
        for (final Comparator<T> comparator : mChain) {
            result = comparator.compare(object1, object2);
            if (result != 0) {
                break;
            }
        }
        return result;
    }
}
