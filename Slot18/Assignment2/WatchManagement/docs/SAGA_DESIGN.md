# Saga Design – Watch Management

## Participants

| Service | Vai trò | Database | Dữ liệu tham gia Saga |
|---|---|---|---|
| MSWatch | Orchestrator | DBWatch | `Watches`, `SagaStates`, `SagaLogs` |
| MSBrand | Participant | DBBrand | `BrandWatches` |

---

## Saga CREATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | CREATE_WATCH – INSERT Watches | MSWatch / DBWatch | DELETE_WATCH | ✔ (`deleteIfExists`) |
| 2 | ASSIGN_BRAND – upsert BrandWatches | MSBrand / DBBrand | UNASSIGN_BRAND | ✔ |

Flow:

```text
CREATE_WATCH
    ↓
ASSIGN_BRAND
    ↓
COMPLETED