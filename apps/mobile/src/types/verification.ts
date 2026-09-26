export type VerificationType = 'STUDENT' | 'ALUMNI';
export type VerificationRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface VerificationRequest {
  id: number;
  userId?: number;
  verificationType: VerificationType;
  status: VerificationRequestStatus;
  proofDocumentUrl?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CreateVerificationRequest {
  verificationType: VerificationType;
  proofDocumentUrl: string;
}
