import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';

import { App } from './app';
import { AccountService } from './services/account';
import { StockService } from './services/stock';
import { TradingService } from './services/trading';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: AccountService,
          useValue: {
            getAccount: () =>
              of({ id: 1, name: 'Test Trader', cashBalance: 100000 }),
          },
        },
        {
          provide: StockService,
          useValue: { getStocks: () => of([]) },
        },
        {
          provide: TradingService,
          useValue: {
            getPortfolio: () => of([]),
            getTransactions: () => of([]),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders the dashboard with the loaded account name', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();

    const heading = fixture.nativeElement.querySelector('h1');
    expect(heading?.textContent).toContain('Test Trader');
  });
});
