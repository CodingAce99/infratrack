import { Observable, of, throwError } from 'rxjs';

import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import {
  Asset,
  CreateAssetRequest,
  UpdateCredentialsRequest,
  UpdateIpRequest,
  UpdateStatusRequest,
} from '../core/models';

/**
 * Lightweight in-memory stand-in for {@link AssetService} used by component
 * tests. Each method returns an `of(...)` by default and records the last call
 * so a test can assert that the component invoked the right service method.
 *
 * Tests that need a custom response reassign the relevant method directly
 * (e.g. `assetService.createAsset = () => throwError(...)`), which keeps the
 * mock/assertion ratio low and the component under test unaware of HTTP.
 */
export class AssetServiceMock {
  refresh = jasmine.createSpy('refresh');
  createAsset = jasmine
    .createSpy('createAsset')
    .and.callFake((_: CreateAssetRequest) => of({ id: 'new-1' } as Asset));
  deleteAsset = jasmine
    .createSpy('deleteAsset')
    .and.callFake((_: string) => of(void 0));
  updateStatus = jasmine
    .createSpy('updateStatus')
    .and.callFake((_id: string, _p: UpdateStatusRequest) => of({} as Asset));
  updateIp = jasmine
    .createSpy('updateIp')
    .and.callFake((_id: string, _p: UpdateIpRequest) => of({} as Asset));
  updateCredentials = jasmine
    .createSpy('updateCredentials')
    .and.callFake((_id: string, _p: UpdateCredentialsRequest) => of({} as Asset));
}

/** Typed provider key mapping the mock to the real service. */
export const ASSET_SERVICE_PROVIDER = {
  provide: AssetService,
  useClass: AssetServiceMock,
};

export { ApiError, Asset };