import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getBooks } from '../api/bookApi';
import { useCart } from '../context/CartContext';
import { useDebouncedValue } from '../lib/useDebouncedValue';
import Pagination from './Pagination';
import type { Book } from '../types/Book';
import type { PagedResponse } from '../types/Page';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'EUR' });
const SEARCH_DEBOUNCE_MS = 350;

export default function BookList() {
  const [searchInput, setSearchInput] = useState('');
  const debouncedSearch = useDebouncedValue(searchInput, SEARCH_DEBOUNCE_MS);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PagedResponse<Book> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { addItem } = useCart();

  // A new search should always start from page 1 — staying on, say, page 3
  // of a search that now has only 1 page of results would show nothing.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);

  useEffect(() => {
    let cancelled = false;

    getBooks({ query: debouncedSearch, page })
      .then((data) => {
        if (!cancelled) {
          setResult(data);
          setError(null);
        }
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load the catalog. Please try again shortly.");
      });

    return () => {
      cancelled = true;
    };
  }, [debouncedSearch, page]);

  if (error) {
    return <p className="state-message state-message--error">{error}</p>;
  }

  return (
    <div className="book-list">
      <input
        type="search"
        className="book-list__search"
        placeholder="Search by title or author…"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        aria-label="Search books"
      />

      {result === null ? (
        <p className="state-message">Loading books…</p>
      ) : result.items.length === 0 ? (
        <p className="state-message">
          {debouncedSearch ? `No books match "${debouncedSearch}".` : 'No books in the catalog yet.'}
        </p>
      ) : (
        <>
          <table className="book-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Author</th>
                <th>Price</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((book) => (
                <tr key={book.id}>
                  <td className="book-table__title">
                    <Link to={`/books/${book.id}`}>{book.title}</Link>
                  </td>
                  <td>{book.author}</td>
                  <td>{currencyFormatter.format(book.price)}</td>
                  <td>
                    <button type="button" onClick={() => addItem(book.id, 1)}>
                      Add to cart
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
