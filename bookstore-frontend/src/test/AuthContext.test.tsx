import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { httpClient } from '../api/httpClient';
import { tokenStore } from '../lib/tokenStore';

function TestConsumer() {
  const { isAuthenticated, error, loginUser, logoutUser } = useAuth();
  return (
    <div>
      <span data-testid="status">{isAuthenticated ? 'authenticated' : 'anonymous'}</span>
      {error && <span data-testid="error">{error}</span>}
      <button onClick={() => loginUser('jane@example.com', 'StrongPass1!').catch(() => {})}>
        Log in
      </button>
      <button onClick={() => logoutUser()}>Log out</button>
    </div>
  );
}

describe('AuthContext', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    tokenStore.clear();
    localStorage.clear();
    mock = new MockAdapter(httpClient);
  });

  it('starts as anonymous when there are no stored tokens', () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('status')).toHaveTextContent('anonymous');
  });

  it('becomes authenticated after a successful login', async () => {
    mock.onPost('/auth/login').reply(200, {
      accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900, mustChangePassword: false,
    });
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    await user.click(screen.getByText('Log in'));

    expect(await screen.findByText('authenticated')).toBeInTheDocument();
  });

  it('surfaces a server error message on failed login without authenticating', async () => {
    mock.onPost('/auth/login').reply(401, { message: 'Invalid email or password' });
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    await user.click(screen.getByText('Log in'));

    expect(await screen.findByTestId('error')).toHaveTextContent('Invalid email or password');
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous');
  });

  it('logout returns to anonymous', async () => {
    tokenStore.setTokens({ accessToken: 'a', refreshToken: 'r' });
    mock.onPost('/auth/logout').reply(204);
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    expect(screen.getByTestId('status')).toHaveTextContent('authenticated');

    await user.click(screen.getByText('Log out'));

    expect(await screen.findByTestId('status')).toHaveTextContent('anonymous');
  });
});
