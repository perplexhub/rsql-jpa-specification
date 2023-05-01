package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

final class JoinUtils {

    private JoinUtils() {
    }

    public static <X, Z> Join<X, ?> getOrCreateJoin(
            final From<Z, X> root, final String attribute, final JoinType joinType) {
        final Join<X, ?> join = getJoin(root, attribute, joinType);
        return join == null ? createJoin(root, attribute, joinType) : join;
    }

    private static <X, Z> Join<X, ?> createJoin(final From<Z, X> root, final String attribute, final JoinType joinType) {
        return joinType == null ? root.join(attribute) : root.join(attribute, joinType);
    }

    private static <X, Z> Join<X, ?> getJoin(
            final From<Z, X> root, final String attribute, final JoinType joinType) {
        final Join<X, ?> fetchJoin = getJoinFromFetches(root, attribute, joinType);
        if (fetchJoin != null) {
            return fetchJoin;
        }
        return getJoinFromJoins(root, attribute, joinType);
    }

    private static <X, Z> Join<X, ?> getJoinFromFetches(
            final From<Z, X> root, final String attribute, final JoinType joinType) {
        for (final Fetch<X, ?> fetch : root.getFetches()) {
            if (Join.class.isAssignableFrom(fetch.getClass()) &&
                    fetch.getAttribute().getName().equals(attribute) &&
                    (joinType == null || fetch.getJoinType().equals(joinType))) {
                return (Join<X, ?>) fetch;
            }
        }
        return null;
    }

    private static <X, Z> Join<X, ?> getJoinFromJoins(
            final From<Z, X> root, final String attribute, final JoinType joinType) {
        for (final Join<X, ?> join : root.getJoins()) {
            if (join.getAttribute().getName().equals(attribute) &&
                    (joinType == null || join.getJoinType().equals(joinType))) {
                return join;
            }
        }
        return null;
    }
}
