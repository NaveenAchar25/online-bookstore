export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  mustChangePassword: boolean;
}

export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
}
