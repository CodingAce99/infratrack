import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { ApiError } from './api-error';
import {
  Asset,
  CreateAssetRequest,
  UpdateCredentialsRequest,
  UpdateIpRequest,
  UpdateStatusRequest,
} from './models';

/**
 * Owns the shared asset-list state for the dashboard.
 *
 * `assets$` is a replayed `BehaviorSubject` seeded with an empty list; multiple
 * dashboard consumers observe the same latest value. `refresh()` revalidates the
 * list from the backend. Successful mutations call `refresh()` to revalidate;
 * failed mutations propagate an `ApiError` and leave the last valid list intact.
 */
@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/assets';

  private readonly assetsSubject = new BehaviorSubject<Asset[]>([]);
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  private readonly errorSubject = new Subject<ApiError>();

  /** Latest asset list, replayed to every subscriber on subscribe. */
  readonly assets$: Observable<Asset[]> = this.assetsSubject.asObservable();
  /** True while a list revalidation request is in flight. */
  readonly loading$: Observable<boolean> = this.loadingSubject.asObservable();
  /** Emitted whenever a load or mutation fails (does not change the list). */
  readonly error$: Observable<ApiError> = this.errorSubject.asObservable();

  /** Revalidate the asset list from the backend. */
  refresh(): void {
    this.loadingSubject.next(true);
    this.http.get<Asset[]>(this.baseUrl).subscribe({
      next: (data) => {
        this.assetsSubject.next(data ?? []);
        this.loadingSubject.next(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorSubject.next(this.toApiError(err));
        this.loadingSubject.next(false);
      },
    });
  }

  /** Create a new asset and revalidate the list on success. */
  createAsset(payload: CreateAssetRequest): Observable<Asset> {
    return this.http.post<Asset>(this.baseUrl, payload).pipe(
      this.mutationGuard(),
      tap(() => this.refresh()),
    );
  }

  /** Update an asset's status and revalidate the list on success. */
  updateStatus(id: string, payload: UpdateStatusRequest): Observable<Asset> {
    return this.http
      .put<Asset>(`${this.baseUrl}/${encodeURIComponent(id)}/status`, payload)
      .pipe(this.mutationGuard(), tap(() => this.refresh()));
  }

  /** Update an asset's IP address and revalidate the list on success. */
  updateIp(id: string, payload: UpdateIpRequest): Observable<Asset> {
    return this.http
      .put<Asset>(`${this.baseUrl}/${encodeURIComponent(id)}/ip`, payload)
      .pipe(this.mutationGuard(), tap(() => this.refresh()));
  }

  /** Update an asset's SSH credentials and revalidate the list on success. */
  updateCredentials(id: string, payload: UpdateCredentialsRequest): Observable<Asset> {
    return this.http
      .put<Asset>(`${this.baseUrl}/${encodeURIComponent(id)}/credentials`, payload)
      .pipe(this.mutationGuard(), tap(() => this.refresh()));
  }

  /** Delete an asset and revalidate the list on success. */
  deleteAsset(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(id)}`).pipe(
      this.mutationGuard(),
      tap(() => this.refresh()),
    );
  }

  /**
   * Converts HTTP failures into `ApiError` and publishes them on `error$`
   * WITHOUT triggering a `refresh()` — failing mutations preserve the last
   * valid list and surface the typed error to callers.
   */
  private mutationGuard<T>(): (
    source: Observable<T>,
  ) => Observable<T> {
    return (source) =>
      source.pipe(
        catchError((err: HttpErrorResponse) => {
          const apiErr = this.toApiError(err);
          this.errorSubject.next(apiErr);
          throw apiErr;
        }),
      );
  }

  private toApiError(err: HttpErrorResponse): ApiError {
    const body = err.error;
    const record = body as Record<string, unknown> | null;
    const message =
      record && typeof record['error'] === 'string'
        ? record['error']
        : `API error: ${err.status}`;
    return new ApiError(err.status, message);
  }
}