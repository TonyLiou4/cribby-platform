export interface PagedResponse<T> {
  content: T[];
  page: number;
  number?: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements?: number;
  first?: boolean;
  last?: boolean;
}
