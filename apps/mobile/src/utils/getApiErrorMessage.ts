import { isAxiosError } from 'axios';

type SpringErrorBody = {
  message?: string;
  error?: string;
  detail?: string;
  title?: string;
};

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong.') {
  if (!isAxiosError<SpringErrorBody>(error)) {
    return fallback;
  }

  const data = error.response?.data;
  return data?.message ?? data?.detail ?? data?.error ?? data?.title ?? error.message ?? fallback;
}
