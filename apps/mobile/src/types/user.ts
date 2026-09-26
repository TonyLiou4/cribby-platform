export type UserType = 'STUDENT' | 'ALUMNI' | 'INCOMING_STUDENT' | 'OTHER';
export type UserRole = 'USER' | 'ADMIN';
export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  universityName?: string | null;
  phoneNumber?: string | null;
  profilePhotoUrl?: string | null;
  bio?: string | null;
  gender?: Gender | null;
  userType: UserType;
  role?: UserRole;
  emailVerified?: boolean;
  studentVerified?: boolean;
  alumniVerified?: boolean;
  phoneVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber?: string | null;
  universityName?: string | null;
  profilePhotoUrl?: string | null;
  bio?: string | null;
  gender?: Gender | null;
  userType: UserType;
}
