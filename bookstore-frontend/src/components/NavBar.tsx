import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export default function NavBar() {
  const { itemCount } = useCart();
  const { isAuthenticated, logoutUser } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logoutUser();
    navigate('/');
  }

  return (
    <header className="nav-bar">
      <Link to="/" className="nav-bar__brand">
        Bookstore
      </Link>
      <nav className="nav-bar__links">
        <Link to="/cart">Cart{itemCount > 0 ? ` (${itemCount})` : ''}</Link>
        {isAuthenticated ? (
          <>
            <Link to="/orders">My orders</Link>
            <button type="button" className="nav-bar__link-button" onClick={handleLogout}>
              Log out
            </button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register">Create account</Link>
          </>
        )}
      </nav>
    </header>
  );
}
