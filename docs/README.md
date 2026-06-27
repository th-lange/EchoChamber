# EchoChamber documentation

Technical reference for the EchoChamber engine. For getting started and day-to-day
development, see the [top-level README](../README.md).

- [Architecture](architecture.md) — layers, component/data flow, hard rules
- [Domain model](domain-model.md) — core entities and value objects
- [Mutation & replay](mutation-and-replay.md) — the mutation chain and async replay loop
- [API reference](api.md) — ingestion, REST resources, action & user endpoints, console
- [Security](security.md) — the two auth chains, roles, accounts, guarantees
- [Database](database.md) — schema and Flyway migrations
- [Testing](testing.md) — how to run tests and the per-layer requirements

Coding standards, layer rules, and the self-correction checklist live in
[../Agent.md](../Agent.md).
