# Solana Wallet Analyzer 🔍

A Scala-based tool for analyzing Solana wallets to identify optimal trading patterns and potential wallets for copy trading.

## Features

- Comprehensive transaction analysis and profit tracking
- Win rate and trade pattern analysis
- Trade duration and size monitoring
- Real-time Solana RPC integration
- Best and worst trade identification
- Trading pattern recognition

## Technical Stack

- Scala 2.13
- STTP HTTP Client
- Play JSON
- Solana RPC API
- Concurrent Programming Support

## Analysis Metrics

The analyzer provides the following wallet statistics:
- 30-day profit analysis
- Win rate percentage
- Average trade size
- Total number of trades
- Best and worst trade performance
- Average position hold time

## Getting Started

### Prerequisites

- Java JDK 11 or higher
- SBT (Scala Build Tool)
- Solana RPC endpoint

### Installation

```bash
git clone https://github.com/yourusername/solana-wallet-analyzer.git
cd solana-wallet-analyzer
sbt clean compile
```

### Usage

Run the analyzer with a wallet address:
```bash
sbt "run <wallet-address>"
```

Example output:
```
=== Wallet Analysis ===
Total Profit (30d): 12.5 SOL
Win Rate: 65.5%
Average Trade Size: 2.3 SOL
Number of Trades: 42
Best Trade: 3.2 SOL
Worst Trade: -0.8 SOL
Average Hold Time: 4.5 hours
==================
```

## Project Structure

```
src/main/scala/com/analyzer/
├── Main.scala                 # Entry point
├── models/
│   ├── Trade.scala           # Trade data structure
│   ├── Transaction.scala     # Transaction data structure
│   └── WalletStats.scala     # Statistics data structure
├── services/
│   ├── AnalyzerService.scala # Analysis logic
│   └── SolanaService.scala   # Solana RPC integration
└── utils/
    └── DateUtils.scala       # Time calculation utilities
```

## Features in Development

- Advanced pattern recognition
- Multiple wallet comparison
- Trading strategy identification
- Risk metrics calculation
- Historical trend analysis
- Performance reporting exports

## Important Notes

- Tool is currently in active development
- Requires a reliable Solana RPC endpoint
- Rate limiting may apply based on RPC provider
- Keep wallet addresses private and secure
- Monitor RPC provider rate limits

## Best Practices

- Use a reliable RPC endpoint
- Consider running analysis during off-peak hours
- Start with smaller date ranges for initial analysis
- Regularly monitor rate limits
- Keep private keys secure

## Contributing

Contributions are welcome. Please feel free to submit a Pull Request.

## License

This project is proprietary software. All rights reserved.

## Support

For issues and feature requests, please open a GitHub issue.