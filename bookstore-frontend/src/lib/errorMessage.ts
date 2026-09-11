/**
 * Pulls the backend's structured error message (ErrorResponse.message) out
 * of an Axios error, falling back to a generic message for anything else
 * (network failure, unexpected shape).
 */
export function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  return fallback;
}
