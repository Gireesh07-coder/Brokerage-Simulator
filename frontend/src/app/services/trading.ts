import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Portfolio {
  id: number;

  account: {
    id: number;
    name: string;
    cashBalance: number;
  };

  stock: {
    id: number;
    symbol: string;
    companyName: string;
    currentPrice: number;
  };

  quantity: number;
}

export interface Transaction {
  id: number;

  account: {
    id: number;
    name: string;
    cashBalance: number;
  };

  stock: {
    id: number;
    symbol: string;
    companyName: string;
    currentPrice: number;
  };

  type: 'BUY' | 'SELL';

  quantity: number;

  price: number;

  tradeValue: number;

  brokerage: number;

  totalAmount: number;

  transactionTime: string;
}

@Injectable({
  providedIn: 'root'
})
export class TradingService {

  private apiUrl = 'http://localhost:8081/api/trading';

  private portfolioApiUrl =
    'http://localhost:8081/api/portfolio';

  private transactionApiUrl =
    'http://localhost:8081/api/transactions';

  constructor(private http: HttpClient) {}

  buy(
    accountId: number,
    stockId: number,
    quantity: number
  ): Observable<Portfolio> {

    return this.http.post<Portfolio>(
      `${this.apiUrl}/buy?accountId=${accountId}&stockId=${stockId}&quantity=${quantity}`,
      {}
    );
  }

  sell(
    accountId: number,
    stockId: number,
    quantity: number
  ): Observable<Portfolio> {

    return this.http.post<Portfolio>(
      `${this.apiUrl}/sell?accountId=${accountId}&stockId=${stockId}&quantity=${quantity}`,
      {}
    );
  }

  getPortfolio(accountId: number): Observable<Portfolio[]> {

    return this.http.get<Portfolio[]>(
      `${this.portfolioApiUrl}/account/${accountId}`
    );
  }

  getTransactions(accountId: number): Observable<Transaction[]> {

    return this.http.get<Transaction[]>(
      `${this.transactionApiUrl}/account/${accountId}`
    );
  }
}