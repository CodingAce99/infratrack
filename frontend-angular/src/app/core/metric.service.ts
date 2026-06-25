import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject, timer } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { ApiError } from './api-error';
import { MetricSnapshot } from './models';

/** Polling interval (milliseconds) for each per-asset metric history stream. */
export const METRIC_POLL_INTERVAL_MS = 60_000;

/** How many snapshots to keep per asset when polling the history endpoint. */
export const METRIC_HISTORY_LIMIT = 20;

/**
 * Provides independent per-asset metric history streams.
 *
 * Each call to `history$(assetId)` returns a freshly scheduled 60-second polling
 * observable that drives its own HTTP request, so a slow or failing asset never
 * blocks collection for another card. Backend failures are converted to an empty
 * snapshot list so the card renders a safe empty state rather than crashing.
 */
@Injectable({ providedIn: 'root' })
export class MetricService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/assets';
  private readonly errorSubject = new Subject<ApiError>();

  readonly error$: Observable<ApiError> = this.errorSubject.asObservable();

  /**
   * @param assetId The asset whose history should be polled.
   * @returns A hot observable that starts with an immediate fetch and re-fetches
   *          every `METRIC_POLL_INTERVAL_MS`. Each subscription schedules its own
   *          timer — independent streams guarantee per-asset fault isolation.
   */
  history$(assetId: string): Observable<MetricSnapshot[]> {
    return timer(0, METRIC_POLL_INTERVAL_MS).pipe(
      switchMap(() =>
        this.http.get<MetricSnapshot[]>(this.historyUrl(assetId)).pipe(
          catchError((err: HttpErrorResponse) => {
            this.errorSubject.next(this.toApiError(err));
            return of<MetricSnapshot[]>([]);
          }),
        ),
      ),
    );
  }

  private historyUrl(assetId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(assetId)}/metrics/history?limit=${METRIC_HISTORY_LIMIT}`;
  }

  private toApiError(err: HttpErrorResponse): ApiError {
    const record = err.error as Record<string, unknown> | null;
    const message =
      record && typeof record['error'] === 'string'
        ? record['error']
        : `API error: ${err.status}`;
    return new ApiError(err.status, message);
  }
}