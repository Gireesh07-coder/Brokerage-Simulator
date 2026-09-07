import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Account } from './services/account';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  name = '';
  initialBalance = 0;

  account: any = null;

  constructor(private accountService: Account) {}

  createAccount() {
    this.accountService
      .createAccount(this.name, this.initialBalance)
      .subscribe({
        next: (response) => {
          this.account = response;
        },
        error: (error) => {
          console.error('Failed to create account:', error);
        }
      });
  }
}