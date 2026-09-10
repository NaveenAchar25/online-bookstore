import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

export default function NavBar() {
  const { itemCount } = useCart();

  return (
    <header className="nav-bar">
      <Link to="/" className="nav-bar__brand">
        Bookstore
      </Link>
      <nav className="nav-bar__links">
        <Link to="/cart">Cart{itemCount > 0 ? ` (${itemCount})` : ''}</Link>
      </nav>
    </header>
  );
}
