# Algorithmic Trading Engine & Automated Market Maker

A high-performance trading simulation platform combining an order-driven exchange with an Automated Market Maker (AMM) liquidity model.

The system continuously generates synthetic market activity through asynchronous trading bots while maintaining real-time market state synchronization across connected clients using WebSockets.

---

## Features

### Automated Market Making

* Implements the Constant Product AMM model (`x * y = k`)
* Supports liquidity pool based pricing
* Calculates slippage dynamically for every trade
* Maintains pool balance during continuous trading activity

### Algorithmic Trading Bots

* Asynchronous trading bots generate synthetic market orders every **1.2 seconds**
* Designed to sustain continuous market activity under high-frequency workloads
* Simulates realistic exchange conditions for testing and experimentation

### Real-Time Market Data

* WebSocket-based price broadcasting
* Live order execution updates
* Multiple clients receive market updates instantly
* Eliminates inefficient polling mechanisms

### Database Optimization

* PostgreSQL persistence layer
* Hibernate ORM for entity management
* Supabase-hosted database deployment
* pgBouncer connection pooling for handling concurrent read/write workloads

---

## Tech Stack

| Component               | Technology  |
| ----------------------- | ----------- |
| Language                | Java 17     |
| Backend Framework       | Spring Boot |
| Database                | PostgreSQL  |
| ORM                     | Hibernate   |
| Real-Time Communication | WebSockets  |
| Hosting                 | Supabase    |
| Connection Pooling      | pgBouncer   |

---

## System Architecture

```text
Trading Bots
      │
      ▼
Synthetic Market Orders
      │
      ▼
Trading Engine
      │
 ┌────┴────┐
 ▼         ▼
Order Book  AMM Pool
 │           │
 └────┬──────┘
      ▼
Trade Execution
      │
      ▼
PostgreSQL Persistence
      │
      ▼
WebSocket Broadcast
      │
      ▼
Connected Clients
```

---

## Constant Product Formula

The AMM uses the classic liquidity invariant:

```text
x * y = k
```

Where:

* `x` = reserve of asset A
* `y` = reserve of asset B
* `k` = constant liquidity invariant

This mechanism automatically adjusts asset prices based on supply and demand while ensuring liquidity remains available.

---

## Performance Considerations

The system was designed with sustained trading workloads in mind:

* Non-blocking asynchronous trade generation
* Connection pooling through pgBouncer
* WebSocket push architecture for low-latency updates
* Efficient database access using Hibernate

---

## Example Workflow

1. Trading bots submit synthetic market orders.
2. The trading engine processes incoming orders.
3. The AMM recalculates pool prices and slippage.
4. Trade execution results are stored in PostgreSQL.
5. Updated market data is broadcast to all connected clients through WebSockets.

---

## Future Improvements

* Limit order support
* Order matching engine integration
* Multiple liquidity pools
* Historical candle generation
* Market depth visualization
* Risk management module
* Benchmarking and latency metrics

---

## Learning Outcomes

This project provided hands-on experience with:

* Market microstructure
* Automated Market Makers
* Exchange infrastructure
* Real-time distributed systems
* Connection pooling strategies
* High-frequency backend workloads
