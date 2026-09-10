import { Route, BrowserRouter, Routes } from 'react-router-dom';
import { CartProvider } from './context/CartContext';
import NavBar from './components/NavBar';
import BooksPage from './pages/BooksPage';
import BookDetailPage from './pages/BookDetailPage';
import CartPage from './pages/CartPage';

export default function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <NavBar />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<BooksPage />} />
            <Route path="/books/:id" element={<BookDetailPage />} />
            <Route path="/cart" element={<CartPage />} />
          </Routes>
        </main>
      </BrowserRouter>
    </CartProvider>
  );
}
