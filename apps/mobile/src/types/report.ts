export type ReportType = 'LISTING';
export type ReportStatus = 'OPEN' | 'REVIEWED' | 'RESOLVED' | 'DISMISSED';

export interface SubmitReportRequest {
  reportType: ReportType;
  reportedListingId: number;
  reason: string;
}

export interface Report {
  id: number;
  reportType: ReportType;
  status: ReportStatus;
  reportedUserId?: number | null;
  reportedListingId?: number | null;
  reason: string;
  createdAt: string;
  updatedAt?: string | null;
}
