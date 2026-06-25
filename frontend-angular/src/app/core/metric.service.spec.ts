import {
  TestBed,
  fakeAsync,
  tick,
  flushMicrotasks,
  discardPeriodicTasks,
} from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { MetricService } from './metric.service';
import { MetricSnapshot } from './models';

describe('MetricService', () => {
  let service: MetricService;
  let httpMock: HttpTestingController;

  const historyUrl = (id: string) =>
    `/api/v1/assets/${encodeURIComponent(id)}/metrics/history?limit=20`;

  const snapshot = (over: Partial<MetricSnapshot> = {}): MetricSnapshot => ({
    assetId: 'a-1',
    cpuUsage: 12.5,
    memoryUsage: 33,
    diskUsage: 50,
    collectedAt: '2026-06-25T12:00:00Z',
    ...over,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MetricService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MetricService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fetches the initial metrics history immediately on subscribe', fakeAsync(() => {
    let last: MetricSnapshot[] = [];
    const sub = service.history$('a-1').subscribe((data) => (last = [...data]));

    // Fire the initial timer(0) emission so HttpClient issues the GET.
    tick(0);
    const req = httpMock.expectOne(historyUrl('a-1'));
    expect(req.request.method).toBe('GET');
    req.flush([snapshot({ assetId: 'a-1', cpuUsage: 55 })]);
    flushMicrotasks();

    sub.unsubscribe();
    discardPeriodicTasks();

    expect(last.length).toBe(1);
    expect(last[0].cpuUsage).toBe(55);
  }));

  it('polls the same history endpoint on a 60-second cadence', fakeAsync(() => {
    const captures: number[][] = [];
    const sub = service.history$('a-1').subscribe((data) => {
      captures.push(data.map((s) => s.cpuUsage));
    });

    // initial fetch
    tick(0);
    httpMock.expectOne(historyUrl('a-1')).flush([snapshot({ cpuUsage: 10 })]);
    flushMicrotasks();

    // advance 60s — timer fires a new GET
    tick(60_000);
    httpMock.expectOne(historyUrl('a-1')).flush([snapshot({ cpuUsage: 20 })]);
    flushMicrotasks();

    // advance another 60s — second poll
    tick(60_000);
    httpMock.expectOne(historyUrl('a-1')).flush([snapshot({ cpuUsage: 30 })]);
    flushMicrotasks();

    sub.unsubscribe();
    flushMicrotasks();
    discardPeriodicTasks();

    // The captured values prove successive polling actually emitted different payloads.
    expect(captures.at(-1)).toEqual([30]);
  }));

  it('uses independent streams per assetId so one card polling does not affect another', fakeAsync(() => {
    let lastA: MetricSnapshot[] = [];
    let lastB: MetricSnapshot[] = [];

    const subA = service.history$('a-1').subscribe((d) => (lastA = [...d]));
    const subB = service.history$('a-2').subscribe((d) => (lastB = [...d]));

    // Each stream emits its own initial GET.
    tick(0);
    const reqA = httpMock.expectOne(historyUrl('a-1'));
    const reqB = httpMock.expectOne(historyUrl('a-2'));
    reqA.flush([snapshot({ assetId: 'a-1', cpuUsage: 11 })]);
    reqB.flush([snapshot({ assetId: 'a-2', cpuUsage: 99 })]);
    flushMicrotasks();

    expect(lastA[0].cpuUsage).toBe(11);
    expect(lastB[0].cpuUsage).toBe(99);

    subA.unsubscribe();
    subB.unsubscribe();
    flushMicrotasks();
    discardPeriodicTasks();
  }));

  it('surfaces an empty history as a safe empty array (no crash)', fakeAsync(() => {
    let last: MetricSnapshot[] = [];
    const sub = service.history$('a-1').subscribe((d) => (last = [...d]));

    tick(0);
    httpMock.expectOne(historyUrl('a-1')).flush([]);
    flushMicrotasks();

    expect(last).toEqual([]);

    sub.unsubscribe();
    flushMicrotasks();
    discardPeriodicTasks();
  }));

  it('emits an empty list on backend error instead of propagating the failure', fakeAsync(() => {
    let last: MetricSnapshot[] = [];
    const sub = service.history$('a-1').subscribe((d) => (last = [...d]));

    tick(0);
    httpMock
      .expectOne(historyUrl('a-1'))
      .flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });
    flushMicrotasks();

    expect(last).toEqual([]);

    sub.unsubscribe();
    flushMicrotasks();
    discardPeriodicTasks();
  }));
});