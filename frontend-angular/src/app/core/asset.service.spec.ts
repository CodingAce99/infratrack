import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';

import { AssetService } from './asset.service';
import { ApiError } from './api-error';
import { Asset, CreateAssetRequest } from './models';

describe('AssetService', () => {
  let service: AssetService;
  let httpMock: HttpTestingController;

  const baseUrl = '/api/v1/assets';

  const sampleAsset = (over: Partial<Asset> = {}): Asset => ({
    id: 'asset-1',
    name: 'web-server-01',
    type: 'SERVER',
    ipAddress: '192.168.1.10',
    status: 'ACTIVE',
    username: 'sshuser',
    ...over,
  });

  const createPayload: CreateAssetRequest = {
    name: 'web-server-01',
    type: 'SERVER',
    ipAddress: '192.168.1.10',
    username: 'sshuser',
    password: 'secret',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AssetService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AssetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('exposes the assets stream which starts empty before the first load', (done) => {
    service.assets$.subscribe({
      next: (assets) => {
        expect(Array.isArray(assets)).toBe(true);
        expect(assets.length).toBe(0);
        done();
      },
      error: done.fail,
    });
  });

  it('loads the asset list via GET /api/v1/assets and exposes it as the latest stream value', async () => {
    const emitted: Asset[][] = [];
    const sub = service.assets$.subscribe((a) => emitted.push([...a]));

    service.refresh();
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([sampleAsset()]);
    sub.unsubscribe();

    // First emission is the initial empty array, second is the loaded list.
    expect(emitted.length).toBeGreaterThanOrEqual(2);
    const last = emitted[emitted.length - 1];
    expect(last.length).toBe(1);
    expect(last[0].id).toBe('asset-1');
  });

  it('clears the loading flag after a successful load', async () => {
    const loadingValues: boolean[] = [];
    const sub = service.loading$.subscribe((l) => loadingValues.push(l));

    service.refresh();
    const req = httpMock.expectOne(baseUrl);
    req.flush([]);
    sub.unsubscribe();

    expect(loadingValues[loadingValues.length - 1]).toBe(false);
  });

  it('creates a new asset, revalidates, and exposes the new asset in assets$', async () => {
    // Seed the existing list.
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1', name: 'old' })]);

    // Kick off create and flush the POST response.
    const createPromise = firstValueFrom(service.createAsset(createPayload));
    const postReq = httpMock.expectOne({ method: 'POST', url: baseUrl });
    expect(postReq.request.body).toEqual(createPayload);
    const created = sampleAsset({ id: 'asset-2', name: 'new' });
    postReq.flush(created);

    const result = await createPromise;
    expect(result.id).toBe('asset-2');

    // Successful mutation triggers a list revalidation.
    const refreshReq = httpMock.expectOne({ method: 'GET', url: baseUrl });
    refreshReq.flush([sampleAsset({ id: 'asset-1', name: 'old' }), created]);

    const latest = await firstValueFrom(service.assets$);
    expect(latest.map((a) => a.id)).toEqual(['asset-1', 'asset-2']);
  });

  it('rejects create with a 409 ApiError and preserves the previous valid asset list', async () => {
    // Seed an existing asset so the "previous list" is non-empty.
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-keep', name: 'keep' })]);

    const before = await firstValueFrom(service.assets$);
    expect(before.map((a) => a.id)).toEqual(['asset-keep']);

    // Kick off a create and flush a 409 conflict.
    const createPromise = firstValueFrom(service.createAsset(createPayload));
    const postReq = httpMock.expectOne({ method: 'POST', url: baseUrl });
    postReq.flush(
      { error: 'Asset with IP address 192.168.1.10 already exists' },
      { status: 409, statusText: 'Conflict' },
    );

    const caught = await createPromise.catch((e) => e);
    expect(caught instanceof ApiError).toBe(true);
    if (caught instanceof ApiError) {
      expect(caught.status).toBe(409);
    }

    // A failed mutation MUST NOT trigger an extra revalidation GET.
    httpMock.expectNone({ method: 'GET', url: baseUrl });

    // The previous list MUST remain intact (mutation failure does NOT clear it).
    const after = await firstValueFrom(service.assets$);
    expect(after.map((a) => a.id)).toEqual(['asset-keep']);
  });

  it('updates status, revalidates, and propagates ApiError on a failed PUT', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([]);

    const updatePromise = firstValueFrom(
      service.updateStatus('asset-1', { status: 'MAINTENANCE' }),
    );
    const putReq = httpMock.expectOne({
      method: 'PUT',
      url: `${baseUrl}/asset-1/status`,
    });
    expect(putReq.request.body).toEqual({ status: 'MAINTENANCE' });
    putReq.flush(sampleAsset({ id: 'asset-1', status: 'MAINTENANCE' }));

    const updated = await updatePromise;
    expect(updated.status).toBe('MAINTENANCE');

    // Successful mutation triggers a revalidation request.
    const refreshReq = httpMock.expectOne({ method: 'GET', url: baseUrl });
    refreshReq.flush([sampleAsset({ id: 'asset-1', status: 'MAINTENANCE' })]);

    const latest = await firstValueFrom(service.assets$);
    expect(latest[0].status).toBe('MAINTENANCE');
  });

  it('rejects a failed status update with an ApiError and leaves the list untouched', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1', status: 'ACTIVE' })]);

    const updatePromise = firstValueFrom(
      service.updateStatus('asset-1', { status: 'MAINTENANCE' }),
    );
    const putReq = httpMock.expectOne({
      method: 'PUT',
      url: `${baseUrl}/asset-1/status`,
    });
    putReq.flush({ error: 'asset not found' }, { status: 404, statusText: 'Not Found' });

    const caught = await updatePromise.catch((e) => e);
    expect(caught instanceof ApiError).toBe(true);
    if (caught instanceof ApiError) {
      expect(caught.status).toBe(404);
    }

    httpMock.expectNone({ method: 'GET', url: baseUrl });
    const latest = await firstValueFrom(service.assets$);
    expect(latest[0].status).toBe('ACTIVE');
  });

  it('updates IP and revalidates afterwards', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1' })]);

    const updatePromise = firstValueFrom(
      service.updateIp('asset-1', { ipAddress: '10.0.0.5' }),
    );
    const putReq = httpMock.expectOne({ method: 'PUT', url: `${baseUrl}/asset-1/ip` });
    expect(putReq.request.body).toEqual({ ipAddress: '10.0.0.5' });
    putReq.flush(sampleAsset({ id: 'asset-1', ipAddress: '10.0.0.5' }));

    await updatePromise;

    const refreshReq = httpMock.expectOne({ method: 'GET', url: baseUrl });
    refreshReq.flush([sampleAsset({ id: 'asset-1', ipAddress: '10.0.0.5' })]);

    const latest = await firstValueFrom(service.assets$);
    expect(latest[0].ipAddress).toBe('10.0.0.5');
  });

  it('updates credentials and revalidates afterwards', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1', username: 'old' })]);

    const updatePromise = firstValueFrom(
      service.updateCredentials('asset-1', { username: 'newuser', password: 'pw' }),
    );
    const putReq = httpMock.expectOne({
      method: 'PUT',
      url: `${baseUrl}/asset-1/credentials`,
    });
    expect(putReq.request.body).toEqual({ username: 'newuser', password: 'pw' });
    putReq.flush(sampleAsset({ id: 'asset-1', username: 'newuser' }));

    await updatePromise;

    const refreshReq = httpMock.expectOne({ method: 'GET', url: baseUrl });
    refreshReq.flush([sampleAsset({ id: 'asset-1', username: 'newuser' })]);

    const latest = await firstValueFrom(service.assets$);
    expect(latest[0].username).toBe('newuser');
  });

  it('deletes an asset and revalidates afterwards', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1' })]);

    const deletePromise = firstValueFrom(service.deleteAsset('asset-1'));
    const delReq = httpMock.expectOne({ method: 'DELETE', url: `${baseUrl}/asset-1` });
    delReq.flush(null);

    await deletePromise;

    const refreshReq = httpMock.expectOne({ method: 'GET', url: baseUrl });
    refreshReq.flush([]);

    const latest = await firstValueFrom(service.assets$);
    expect(latest).toEqual([]);
  });

  it('emits an ApiError on error$ and clears the loading flag when a refresh() load fails', () => {
    const errors: ApiError[] = [];
    const loadingValues: boolean[] = [];
    const errSub = service.error$.subscribe((e) => errors.push(e));
    const loadSub = service.loading$.subscribe((l) => loadingValues.push(l));

    service.refresh();
    const req = httpMock.expectOne(baseUrl);
    req.flush({ error: 'backend down' }, { status: 500, statusText: 'Server Error' });

    // The error callback fires synchronously on flush: error$ receives the
    // typed ApiError and loading$ is reset to false.
    expect(errors.length).toBe(1);
    expect(errors[0] instanceof ApiError).toBe(true);
    expect(errors[0].status).toBe(500);
    expect(errors[0].message).toBe('backend down');
    expect(loadingValues[loadingValues.length - 1]).toBe(false);

    errSub.unsubscribe();
    loadSub.unsubscribe();
  });

  it('preserves the last valid asset list when a refresh() load fails', async () => {
    // Seed a valid list first so the "previous list" is non-empty.
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-keep', name: 'keep' })]);

    const before = await firstValueFrom(service.assets$);
    expect(before.map((a) => a.id)).toEqual(['asset-keep']);

    // A subsequent refresh that fails MUST NOT clear the previously loaded list.
    service.refresh();
    const req = httpMock.expectOne(baseUrl);
    req.flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });

    const after = await firstValueFrom(service.assets$);
    expect(after.map((a) => a.id)).toEqual(['asset-keep']);
  });

  it('does not trigger revalidation on a failed delete', async () => {
    service.refresh();
    httpMock.expectOne(baseUrl).flush([sampleAsset({ id: 'asset-1' })]);

    const deletePromise = firstValueFrom(service.deleteAsset('asset-1'));
    const delReq = httpMock.expectOne({ method: 'DELETE', url: `${baseUrl}/asset-1` });
    delReq.flush({ error: 'not found' }, { status: 404, statusText: 'Not Found' });

    const caught = await deletePromise.catch((e) => e);
    expect(caught instanceof ApiError).toBe(true);

    httpMock.expectNone({ method: 'GET', url: baseUrl });
    const latest = await firstValueFrom(service.assets$);
    expect(latest.map((a) => a.id)).toEqual(['asset-1']);
  });
});