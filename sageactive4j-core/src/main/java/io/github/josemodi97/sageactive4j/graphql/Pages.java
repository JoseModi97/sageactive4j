package io.github.josemodi97.sageactive4j.graphql;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Lazily walks every page of a cursor-paginated list: the next page is only
 * requested when iteration reaches the end of the current one, so breaking
 * out of a loop early costs nothing extra.
 *
 * <pre>{@code
 * for (Customer c : sage.thirdParties().allCustomers(ListOptions.first(500))) { ... }
 * }</pre>
 */
public final class Pages {

    private Pages() {
    }

    /**
     * @param first     the options for the first page; {@code after} is then advanced automatically
     * @param fetchPage fetches one page for the given options
     */
    public static <T> Iterable<T> iterate(final ListOptions first, final Function<ListOptions, Connection<T>> fetchPage) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new PageIterator<T>(first == null ? ListOptions.defaults() : first, fetchPage);
            }
        };
    }

    private static final class PageIterator<T> implements Iterator<T> {
        private final Function<ListOptions, Connection<T>> fetchPage;
        private ListOptions nextOptions;
        private Iterator<T> current;
        private boolean exhausted;

        PageIterator(ListOptions first, Function<ListOptions, Connection<T>> fetchPage) {
            this.fetchPage = fetchPage;
            this.nextOptions = first;
        }

        @Override
        public boolean hasNext() {
            while ((current == null || !current.hasNext()) && !exhausted) {
                Connection<T> page = fetchPage.apply(nextOptions);
                current = page.iterator();
                String cursor = page.getPageInfo().getEndCursor();
                if (page.hasNextPage() && cursor != null && !cursor.equals(nextOptions.getAfter())) {
                    nextOptions = nextOptions.after(cursor);
                } else {
                    // Also stops on a repeated cursor, so a misbehaving server can't loop us forever.
                    exhausted = true;
                }
            }
            return current != null && current.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
