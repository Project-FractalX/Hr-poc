# 🗄️ Data Architecture: leave-service

This service has been upgraded by FractalX to support **Distributed Data Isolation**.

## 1. Database Configuration
This service follows the **Database-per-Service** pattern.

| Property | Value |
| :--- | :--- |
| **Database Type** | H2 (In-Memory) |
| **Connection URL** | `jdbc:h2:mem:leave_service` |
| **Schema Strategy** | `ddl-auto: update` (Hibernate managed) |

> **⚠️ Note:** This service uses an **In-Memory H2 Database** (Default).
> - Data is lost when the service stops.
> - To use MySQL/Postgres, see the **Configuration Guide** below.

## 2. Decoupling Strategy
Cross-service relationships have been transformed at the Java level to remove hard Foreign Key constraints.

- **Local Entities**: Relationships inside this service (e.g., `@OneToMany`) are preserved. Foreign Keys exist.
- **Remote Entities**: Relationships to other services were converted to IDs (e.g., `Customer customer` → `String customerId`).

## 3. Injected Dependencies
FractalX automatically injected the following driver into `pom.xml`:
- **Driver Class**: `org.h2.Driver`

## 4. How to Verify
1. Start the service: `mvn spring-boot:run`
2. Check logs for `Hibernate: create table ...`
3. Access H2 Console: `http://localhost:8083/h2-console`
   - JDBC URL: `jdbc:h2:mem:leave_service`

## 5. Configuration Guide
To switch this service to a physical database (MySQL/PostgreSQL), add the following block to your **Monolith's** `application.yml` before decomposition:

```yaml
fractalx:
  modules:
    # Must match the service name exactly
    leave-service:
      datasource:
        url: jdbc:mysql://localhost:3306/leave_service_db
        username: root
        password: <your_password>
        driver-class-name: com.mysql.cj.jdbc.Driver
      jpa:
        hibernate:
          ddl-auto: update
```

## 6. Distributed Saga Participation

This service participates in **1** distributed saga(s) coordinated by `fractalx-saga-orchestrator` running on **port 8099**.

> **How it works:** The saga orchestrator calls each participating service via **NetScope gRPC** in a defined sequence. If any step fails, the orchestrator calls compensation methods in **reverse order** to roll back changes.

### `approve-leave-saga` — **Owner** 👑

> Approves employee leave: marks employee on leave and deducts pay.

This service **owns** this saga. The `@DistributedSaga` annotation was found in `LeaveModule.approveLeave()` and the orchestrator service was auto-generated from it.

**To trigger this saga**, POST to the orchestrator instead of calling services directly:

```bash
curl -X POST http://localhost:8099/saga/approve-leave-saga/start \
  -H "Content-Type: application/json" \
  -d '{\"employeeId\":1,\"leaveType\":\"example\",\"startDate\":\"example\",\"endDate\":\"example\",\"leaveDays\":1,\"reason\":\"example\"}'
```

**Payload fields** (`ApproveLeaveSagaPayload`):

| Field | Type |
| :--- | :--- |
| `employeeId` | `Long` |
| `leaveType` | `String` |
| `startDate` | `String` |
| `endDate` | `String` |
| `leaveDays` | `Integer` |
| `reason` | `String` |

**Execution sequence managed by the orchestrator:**

1. `employee-service` → `markOnLeave()`  ↩ compensate: `cancelMarkOnLeave()`
2. `payroll-service` → `deductLeave()`  ↩ compensate: `cancelDeductLeave()`

> **⚠️ Important:** Do **not** call `EmployeeService` or other saga participants directly in your business logic. The orchestrator now manages the full call sequence, state tracking, and rollback.

**List all active sagas:**

```bash
curl http://localhost:8099/saga
```
