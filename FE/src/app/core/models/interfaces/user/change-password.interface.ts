export interface VerifyPasswordChangeCodeRequest {
  code: string;
}

export interface ChangeCurrentUserPasswordRequest {
  verificationToken: string;
  newPassword: string;
  confirmPassword: string;
}
