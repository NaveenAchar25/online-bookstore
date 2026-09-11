import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from '../pages/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import * as authApi from '../api/authApi';

vi.mock('../api/authApi');

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows validation errors when submitted empty, without calling the API', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(await screen.findByText(/enter your email/i)).toBeInTheDocument();
    expect(screen.getByText(/enter your password/i)).toBeInTheDocument();
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('submits valid credentials to the API', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900, mustChangePassword: false,
    });
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'StrongPass1!');
    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(authApi.login).toHaveBeenCalledWith('jane@example.com', 'StrongPass1!');
  });

  it('shows the server error message on failed login', async () => {
    vi.mocked(authApi.login).mockRejectedValue({ response: { data: { message: 'Invalid email or password' } } });
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'WrongPassword1!');
    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
  });
});
