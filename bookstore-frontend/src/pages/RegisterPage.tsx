import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Mirrors the backend's RegisterRequest validation so the user gets instant
// feedback — but the backend re-validates independently.
const PASSWORD_PATTERN = /^(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).*$/;

interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

interface FieldErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
}

export default function RegisterPage() {
  const { registerUser, error: serverError } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>({ firstName: '', lastName: '', email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [submitting, setSubmitting] = useState(false);

  function updateField(field: keyof FormState) {
    return (event: ChangeEvent<HTMLInputElement>) =>
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!form.firstName.trim()) errors.firstName = 'Enter your first name.';
    if (!form.lastName.trim()) errors.lastName = 'Enter your last name.';
    if (!form.email.trim()) errors.email = 'Enter your email address.';
    if (form.password.length < 8) {
      errors.password = 'Password must be at least 8 characters.';
    } else if (!PASSWORD_PATTERN.test(form.password)) {
      errors.password = 'Password must contain at least one number and one special character.';
    }
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await registerUser(form);
      navigate('/login');
    } catch {
      // Server error message is already surfaced via the auth context's `error`.
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <h1>Create an account</h1>
      <form onSubmit={handleSubmit} noValidate>
        <label htmlFor="register-first-name">First name</label>
        <input id="register-first-name" value={form.firstName} onChange={updateField('firstName')} />
        {fieldErrors.firstName && <p className="field-error">{fieldErrors.firstName}</p>}

        <label htmlFor="register-last-name">Last name</label>
        <input id="register-last-name" value={form.lastName} onChange={updateField('lastName')} />
        {fieldErrors.lastName && <p className="field-error">{fieldErrors.lastName}</p>}

        <label htmlFor="register-email">Email</label>
        <input id="register-email" type="email" value={form.email} onChange={updateField('email')} />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <label htmlFor="register-password">Password</label>
        <input id="register-password" type="password" value={form.password} onChange={updateField('password')} />
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        {serverError && <p className="field-error">{serverError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </div>
  );
}
