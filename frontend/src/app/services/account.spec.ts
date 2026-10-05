import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { AccountService } from './account';

describe('AccountService', () => {
  let service: AccountService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AccountService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AccountService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('loads an account from the API', () => {
    const account = { id: 1, name: 'Test Trader', cashBalance: 100000 };
    let result: typeof account | undefined;

    service.getAccount(1).subscribe((value) => {
      result = value;
    });

    const request = httpTestingController.expectOne(
      'http://localhost:8081/api/accounts/1',
    );
    expect(request.request.method).toBe('GET');
    request.flush(account);

    expect(result).toEqual(account);
  });
});
