import { useMutation } from '@tanstack/react-query';

import { submitReport } from '@/api/reports';
import type { SubmitReportRequest } from '@/types';

export function useSubmitReport() {
  return useMutation({
    mutationFn: (payload: SubmitReportRequest) => submitReport(payload),
  });
}
