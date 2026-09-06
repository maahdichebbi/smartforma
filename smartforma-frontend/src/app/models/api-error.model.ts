export interface ApiErrorResponse {
  status: number;
  message: string;
  timestamp?: string;
  errors?: Record<string, string>;
}
