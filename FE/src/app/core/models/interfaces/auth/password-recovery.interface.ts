export interface ForgotPasswordRequest {
  email: string;
}

export interface VerifyPasswordResetCodeRequest {
  email: string;
  code: string;
}

export interface PasswordCodeVerificationResponse {
  verificationToken: string;
  message: string;
}

export interface VerifiedPasswordResetRequest {
  verificationToken: string;
  newPassword: string;
  confirmPassword: string;
}
