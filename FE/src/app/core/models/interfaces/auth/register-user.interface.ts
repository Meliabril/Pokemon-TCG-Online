import { UserRole } from '../../enums/user/user-role.enum';
import { UserStatus } from '../../enums/user/user-status.enum';

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  avatar?: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  username: string;
  role: UserRole;
  status: UserStatus;
  emailVerified: boolean;
  avatar?: string;
  message?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VerifyAccountRequest {
  email: string;
  code: string;
}

export interface ResendVerificationCodeRequest {
  email: string;
}

export interface GenericMessageResponse {
  message: string;
}
