import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AccountService, Account } from './services/account';
import { StockService, Stock } from './services/stock';

import {
  TradingService,
  Portfolio,
  Transaction
} from './services/trading';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {

  accountId = 1;

  account: Account | null = null;

  stocks: Stock[] = [];

  portfolio: Portfolio[] = [];

  transactions: Transaction[] = [];

  selectedStockId = 1;

  quantity = 1;

  depositAmount = 0;

  message = '';

  messageType: 'success' | 'error' = 'success';

  showAddFundsModal = false;

  isLoading = true;

  isTrading = false;


  constructor(
    private accountService: AccountService,
    private stockService: StockService,
    private tradingService: TradingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}


  ngOnInit(): void {

    this.loadDashboard();

  }


  loadDashboard(): void {

    this.isLoading = true;

    this.loadAccount();

    this.loadStocks();

    this.loadPortfolio();

    this.loadTransactions();

  }


  loadAccount(): void {

    this.accountService
      .getAccount(this.accountId)
      .subscribe({

        next: (account) => {

          console.log('Account loaded:', account);

          this.account = account;

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(
            'Failed to load account:',
            error
          );

          this.showMessage(
            'Unable to load account',
            'error'
          );

        }

      });

  }


  loadStocks(): void {

    this.stockService
      .getStocks()
      .subscribe({

        next: (stocks) => {

          console.log(
            'Stocks loaded:',
            stocks
          );

          this.stocks = stocks;

          if (stocks.length > 0) {

            this.selectedStockId = stocks[0].id;

          }

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(
            'Failed to load stocks:',
            error
          );

          this.showMessage(
            'Unable to load market data',
            'error'
          );

        }

      });

  }


  loadPortfolio(): void {

    this.tradingService
      .getPortfolio(this.accountId)
      .subscribe({

        next: (portfolio) => {

          console.log(
            'Portfolio loaded:',
            portfolio
          );

          this.portfolio = portfolio;

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(
            'Failed to load portfolio:',
            error
          );

        }

      });

  }


  loadTransactions(): void {

    this.tradingService
      .getTransactions(this.accountId)
      .subscribe({

        next: (transactions) => {

          console.log(
            'Transactions loaded:',
            transactions
          );

          this.transactions = transactions;

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(
            'Failed to load transactions:',
            error
          );

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();

        }

      });

  }


  selectStock(stockId: number): void {

    this.selectedStockId = stockId;

    this.changeDetectorRef.detectChanges();

  }


  get selectedStock(): Stock | null {

    return this.stocks.find(
      stock => stock.id === this.selectedStockId
    ) ?? null;

  }


  get tradeValue(): number {

    if (
      !this.selectedStock ||
      this.quantity <= 0
    ) {

      return 0;

    }

    return (
      this.selectedStock.currentPrice *
      this.quantity
    );

  }


  get estimatedBrokerage(): number {

    return this.tradeValue * 0.001;

  }


  get estimatedBuyTotal(): number {

    return (
      this.tradeValue +
      this.estimatedBrokerage
    );

  }


  get estimatedSellTotal(): number {

    return (
      this.tradeValue -
      this.estimatedBrokerage
    );

  }


  get portfolioValue(): number {

    return this.portfolio.reduce(

      (total, holding) =>

        total +
        (
          holding.stock.currentPrice *
          holding.quantity
        ),

      0

    );

  }


  get totalAccountValue(): number {

    return (
      (this.account?.cashBalance ?? 0) +
      this.portfolioValue
    );

  }


  get marketStatus(): 'OPEN' | 'CLOSED' {

    const now = new Date();

    const day = now.getDay();

    if (
      day === 0 ||
      day === 6
    ) {

      return 'CLOSED';

    }

    const hours = now.getHours();

    const minutes = now.getMinutes();

    const currentMinutes =
      hours * 60 + minutes;

    const marketOpen =
      9 * 60 + 15;

    const marketClose =
      15 * 60 + 30;

    return (
      currentMinutes >= marketOpen &&
      currentMinutes < marketClose
    )
      ? 'OPEN'
      : 'CLOSED';

  }


  get marketStatusText(): string {

    return this.marketStatus === 'OPEN'
      ? 'Market Open'
      : 'Market Closed';

  }


  get marketStatusDescription(): string {

    return this.marketStatus === 'OPEN'

      ? 'Simulated market · Orders available anytime'

      : 'Simulated market · Orders available anytime';

  }


  openAddFunds(): void {

    this.depositAmount = 0;

    this.showAddFundsModal = true;

    this.changeDetectorRef.detectChanges();

  }


  closeAddFunds(): void {

    this.showAddFundsModal = false;

    this.depositAmount = 0;

    this.changeDetectorRef.detectChanges();

  }


  addMoney(): void {

    if (this.depositAmount <= 0) {

      this.showMessage(
        'Enter an amount greater than ₹0',
        'error'
      );

      return;

    }

    const amount =
      this.depositAmount;

    this.accountService
      .deposit(
        this.accountId,
        amount
      )
      .subscribe({

        next: (account) => {

          this.account = account;

          this.showAddFundsModal = false;

          this.depositAmount = 0;

          this.showMessage(
            `₹${amount.toLocaleString('en-IN')} added successfully`,
            'success'
          );

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(error);

          this.showMessage(

            error.error?.message ||
            'Failed to add money',

            'error'

          );

        }

      });

  }


  buyStock(): void {

    if (this.quantity <= 0) {

      this.showMessage(
        'Quantity must be greater than zero',
        'error'
      );

      return;

    }


    this.isTrading = true;


    this.tradingService
      .buy(
        this.accountId,
        this.selectedStockId,
        this.quantity
      )
      .subscribe({

        next: (portfolio) => {

          const tradedQuantity =
            this.quantity;

          const symbol =
            portfolio.stock.symbol;

          this.account =
            portfolio.account;

          this.showMessage(

            `Bought ${tradedQuantity} ${symbol} share${tradedQuantity > 1 ? 's' : ''} successfully`,

            'success'

          );

          this.loadAccount();

          this.loadPortfolio();

          this.loadTransactions();

          this.isTrading = false;

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(error);

          this.showMessage(

            error.error?.message ||
            'Failed to buy stock',

            'error'

          );

          this.isTrading = false;

          this.changeDetectorRef.detectChanges();

        }

      });

  }


  sellStock(): void {

    if (this.quantity <= 0) {

      this.showMessage(
        'Quantity must be greater than zero',
        'error'
      );

      return;

    }


    this.isTrading = true;


    this.tradingService
      .sell(
        this.accountId,
        this.selectedStockId,
        this.quantity
      )
      .subscribe({

        next: (portfolio) => {

          const tradedQuantity =
            this.quantity;

          const symbol =
            portfolio.stock.symbol;

          this.account =
            portfolio.account;

          this.showMessage(

            `Sold ${tradedQuantity} ${symbol} share${tradedQuantity > 1 ? 's' : ''} successfully`,

            'success'

          );

          this.loadAccount();

          this.loadPortfolio();

          this.loadTransactions();

          this.isTrading = false;

          this.changeDetectorRef.detectChanges();

        },

        error: (error) => {

          console.error(error);

          this.showMessage(

            error.error?.message ||
            'Failed to sell stock',

            'error'

          );

          this.isTrading = false;

          this.changeDetectorRef.detectChanges();

        }

      });

  }


  showMessage(
    message: string,
    type: 'success' | 'error'
  ): void {

    this.message = message;

    this.messageType = type;

    this.changeDetectorRef.detectChanges();


    setTimeout(() => {

      this.message = '';

      this.changeDetectorRef.detectChanges();

    }, 4000);

  }

}