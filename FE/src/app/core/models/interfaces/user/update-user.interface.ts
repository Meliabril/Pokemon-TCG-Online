export interface UserProfile {
  username: string;
  email: string;
  emailVerified: boolean;
  avatar?: string;
}

export interface UpdateUserRequest {
  username?: string;
  avatar?: string;
}
