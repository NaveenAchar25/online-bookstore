import { Route, BrowserRouter, Routes } from 'react-router-dom';
import BooksPage from './pages/BooksPage';
import BookDetailPage from './pages/BookDetailPage';

export default function App() {
  return (
    <BrowserRouter>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<BooksPage />} />
          <Route path="/books/:id" element={<BookDetailPage />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}