import { apiClient } from './client';

import type { Report, SubmitReportRequest } from '@/types';

export async function submitReport(payload: SubmitReportRequest) {
  const response = await apiClient.post<Report>('/api/reports', {
    reportType: payload.reportType,
    reportedListingId: payload.reportedListingId,
    reason: payload.reason.trim(),
  });

  return response.data;
}
