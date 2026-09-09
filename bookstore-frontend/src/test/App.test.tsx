import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from '../App';

describe('App', () => {
  it('renders the home page placeholder', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Bookstore' })).toBeInTheDocument();
    expect(screen.getByText(/Sprint 1/)).toBeInTheDocument();
  });
});
