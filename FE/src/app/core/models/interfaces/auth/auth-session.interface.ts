import { UserRole } from '../../enums/user/user-role.enum';
import { UserStatus } from '../../enums/user/user-status.enum';

export interface AuthUser {
  id: string;
  email: string;
  username: string;
  role: UserRole;
  status: UserStatus;
  emailVerified: boolean;
  avatar?: string;
  matchmakingCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  accessTokenExpiresAt?: number;
  user: AuthUser | null;
}

export type AuthStatus = 'checking' | 'authenticated' | 'unauthenticated';

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LogoutResponse {
  message: string;
}
