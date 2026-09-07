import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class Account {

  private apiUrl = 'http://localhost:8081/api/accounts';

  constructor(private http: HttpClient) {}

  createAccount(name: string, initialBalance: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}?name=${name}&initialBalance=${initialBalance}`,
      {}
    );
  }
}