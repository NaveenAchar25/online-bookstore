import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RegisterPage from '../pages/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import * as authApi from '../api/authApi';

vi.mock('../api/authApi');

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/first name/i), 'Jane');
  await user.type(screen.getByLabelText(/last name/i), 'Doe');
  await user.type(screen.getByLabelText(/email/i), 'jane@example.com');
  await user.type(screen.getByLabelText(/password/i), 'StrongPass1!');
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('rejects a password with no digit or special character before hitting the API', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await user.type(screen.getByLabelText(/first name/i), 'Jane');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'weakpassword');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/at least one number and one special character/i)).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
  });

  it('submits a valid registration to the API', async () => {
    vi.mocked(authApi.register).mockResolvedValue({
      id: 1, email: 'jane@example.com', firstName: 'Jane', lastName: 'Doe',
    });
    const user = userEvent.setup();
    renderRegisterPage();

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(authApi.register).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.com',
      password: 'StrongPass1!',
    });
  });

  it('shows a duplicate-email server error', async () => {
    vi.mocked(authApi.register).mockRejectedValue({
      response: { data: { message: 'An account with email jane@example.com already exists' } },
    });
    const user = userEvent.setup();
    renderRegisterPage();

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/already exists/i)).toBeInTheDocument();
  });
});
