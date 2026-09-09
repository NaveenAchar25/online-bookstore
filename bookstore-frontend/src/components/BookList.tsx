import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getBooks } from '../api/bookApi';
import type { Book } from '../types/Book';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export default function BookList() {
  const [books, setBooks] = useState<Book[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getBooks()
      .then((data) => {
        if (!cancelled) setBooks(data);
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load the catalog. Please try again shortly.");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return <p className="state-message state-message--error">{error}</p>;
  }

  if (books === null) {
    return <p className="state-message">Loading books…</p>;
  }

  if (books.length === 0) {
    return <p className="state-message">No books in the catalog yet.</p>;
  }

  return (
    <table className="book-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Author</th>
          <th>Price</th>
        </tr>
      </thead>
      <tbody>
        {books.map((book) => (
          <tr key={book.id}>
            <td className="book-table__title">
              <Link to={`/books/${book.id}`}>{book.title}</Link>
            </td>
            <td>{book.author}</td>
            <td>{currencyFormatter.format(book.price)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
