import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { getBookById } from '../api/bookApi';
import { useCart } from '../context/CartContext';
import type { Book } from '../types/Book';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

type LoadState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | { status: 'loaded'; book: Book };

export default function BookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [state, setState] = useState<LoadState>({ status: 'loading' });
  const [quantityInput, setQuantityInput] = useState('1');
  const { addItem, error: cartError } = useCart();

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });

    getBookById(Number(id))
      .then((book) => {
        if (!cancelled) setState({ status: 'loaded', book });
      })
      .catch((err) => {
        if (cancelled) return;
        // A 404 means "this book genuinely doesn't exist" — a different
        // user-facing message than "something is wrong with the request",
        // which covers everything else (network failure, server error).
        const isNotFound = isAxiosError(err) && err.response?.status === 404;
        setState({ status: isNotFound ? 'not-found' : 'error' });
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  if (state.status === 'loading') {
    return <p className="state-message">Loading book…</p>;
  }

  if (state.status === 'not-found') {
    return (
      <div className="book-detail-page">
        <p className="state-message state-message--error">Couldn't find that book.</p>
        <Link to="/">Back to the catalog</Link>
      </div>
    );
  }

  if (state.status === 'error') {
    return <p className="state-message state-message--error">Something went wrong loading this book.</p>;
  }

  const { book } = state;

  return (
    <div className="book-detail-page">
      <Link to="/" className="book-detail-page__back-link">
        &larr; Back to the catalog
      </Link>
      <h1>{book.title}</h1>
      <p className="book-detail-page__author">{book.author}</p>
      <p className="book-detail-page__price">{currencyFormatter.format(book.price)}</p>
      {book.description && <p className="book-detail-page__description">{book.description}</p>}
      <p className="book-detail-page__stock">
        {book.stockQuantity > 0 ? `${book.stockQuantity} in stock` : 'Out of stock'}
      </p>
      {book.stockQuantity > 0 && (
        <div className="book-detail-page__add-to-cart">
          <input
            type="number"
            min={1}
            max={book.stockQuantity}
            value={quantityInput}
            onChange={(e) => setQuantityInput(e.target.value)}
            aria-label="Quantity"
          />
          <button
            type="button"
            onClick={() => {
              const parsed = Math.max(1, Number(quantityInput) || 1);
              addItem(book.id, parsed);
            }}
          >
            Add to cart
          </button>
        </div>
      )}
      {cartError && <p className="field-error">{cartError}</p>}
    </div>
  );
}
