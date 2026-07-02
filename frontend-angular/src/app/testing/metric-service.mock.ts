import { Observable, of, throwError } from 'rxjs';

import { MetricService } from '../core/metric.service';
import { ApiError } from '../core/api-error';
import { MetricSnapshot } from '../core/models';

/**
 * In-memory stand-in for {@link MetricService} used by component tests. Each
 * call to `history$` returns `of([])` by default; tests reassign `history$` to
 * a function returning a custom stream so a card under test never touches HTTP.
 */
export class MetricServiceMock {
  history$ = (_assetId: string): Observable<MetricSnapshot[]> => of([]);
}

export const METRIC_SERVICE_PROVIDER = {
  provide: MetricService,
  useClass: MetricServiceMock,
};

export { ApiError };