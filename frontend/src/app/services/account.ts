import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Account {
  id: number;
  name: string;
  cashBalance: number;
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private apiUrl = 'http://localhost:8081/api/accounts';

  constructor(private http: HttpClient) {}

  createAccount(name: string, initialBalance: number): Observable<Account> {
    return this.http.post<Account>(
      `${this.apiUrl}?name=${name}&initialBalance=${initialBalance}`,
      {}
    );
  }

  getAccount(id: number): Observable<Account> {
    return this.http.get<Account>(`${this.apiUrl}/${id}`);
  }

  deposit(id: number, amount: number): Observable<Account> {
    return this.http.post<Account>(
      `${this.apiUrl}/${id}/deposit?amount=${amount}`,
      {}
    );
  }
}