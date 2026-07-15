import { UserRole } from '../../enums/user/user-role.enum';
import { UserStatus } from '../../enums/user/user-status.enum';

export interface CurrentUser {
  id: string;
  email: string;
  username: string;
  avatar?: string;
  role: UserRole;
  status: UserStatus;
  emailVerified: boolean;
  matchmakingCode: string;
  createdAt: string;
  updatedAt: string;
}
