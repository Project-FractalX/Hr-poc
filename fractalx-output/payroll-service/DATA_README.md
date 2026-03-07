# 🗄️ Data Architecture: payroll-service

This service has been upgraded by FractalX to support **Distributed Data Isolation**.

## 1. Database Configuration
This service follows the **Database-per-Service** pattern.

| Property | Value |
| :--- | :--- |
| **Database Type** | H2 (In-Memory) |
| **Connection URL** | `jdbc:h2:mem:payroll_service` |
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
3. Access H2 Console: `http://localhost:8082/h2-console`
   - JDBC URL: `jdbc:h2:mem:payroll_service`

## 5. Configuration Guide
To switch this service to a physical database (MySQL/PostgreSQL), add the following block to your **Monolith's** `application.yml` before decomposition:

```yaml
fractalx:
  modules:
    # Must match the service name exactly
    payroll-service:
      datasource:
        url: jdbc:mysql://localhost:3306/payroll_service_db
        username: root
        password: <your_password>
        driver-class-name: com.mysql.cj.jdbc.Driver
      jpa:
        hibernate:
          ddl-auto: update
```

## 6. Distributed Saga Participation

This service participates in **2** distributed saga(s) coordinated by `fractalx-saga-orchestrator` running on **port 8099**.

> **How it works:** The saga orchestrator calls each participating service via **NetScope gRPC** in a defined sequence. If any step fails, the orchestrator calls compensation methods in **reverse order** to roll back changes.

### `onboard-employee-saga` — **Participant** 🔗

> Onboards a new employee: sets up payroll and assigns to department.

The saga orchestrator calls this service's methods via **NetScope gRPC on port 18082**.

| Step # | Forward Call | Compensation | Triggered When |
| :---: | :--- | :--- | :--- |
| 1 | `setupPayroll()` | `cancelSetupPayroll()` | Step 1 of the saga |

**What to expect at runtime:**

- The orchestrator serialises the saga payload and dispatches calls over gRPC.
- Parameters are extracted from the original saga payload and matched by name.
- If this service throws an exception, the orchestrator will invoke the corresponding **compensation method** to undo the side-effect.

**Track the saga execution that called this service:**

```bash
# The correlationId is returned when the saga owner starts the saga
curl http://localhost:8099/saga/status/<correlationId>
```

### `approve-leave-saga` — **Participant** 🔗

> Approves employee leave: marks employee on leave and deducts pay.

The saga orchestrator calls this service's methods via **NetScope gRPC on port 18082**.

| Step # | Forward Call | Compensation | Triggered When |
| :---: | :--- | :--- | :--- |
| 2 | `deductLeave()` | `cancelDeductLeave()` | Step 2 of the saga |

**What to expect at runtime:**

- The orchestrator serialises the saga payload and dispatches calls over gRPC.
- Parameters are extracted from the original saga payload and matched by name.
- If this service throws an exception, the orchestrator will invoke the corresponding **compensation method** to undo the side-effect.

**Track the saga execution that called this service:**

```bash
# The correlationId is returned when the saga owner starts the saga
curl http://localhost:8099/saga/status/<correlationId>
```

**List all active sagas:**

```bash
curl http://localhost:8099/saga
```
