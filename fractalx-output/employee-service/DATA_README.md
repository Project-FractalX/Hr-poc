# 🗄️ Data Architecture: employee-service

This service has been upgraded by FractalX to support **Distributed Data Isolation**.

## 1. Database Configuration
This service follows the **Database-per-Service** pattern.

| Property | Value |
| :--- | :--- |
| **Database Type** | H2 (Default) |
| **Connection URL** | `jdbc:h2:mem:employee_service` |
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
3. Access H2 Console: `http://localhost:8081/h2-console`
   - JDBC URL: `jdbc:h2:mem:employee_service`

## 5. Configuration Guide
To switch this service to a physical database (MySQL/PostgreSQL), add the following block to your **Monolith's** `application.yml` before decomposition:

```yaml
fractalx:
  modules:
    # Must match the service name exactly
    employee-service:
      datasource:
        url: jdbc:mysql://localhost:3306/employee_service_db
        username: root
        password: <your_password>
        driver-class-name: com.mysql.cj.jdbc.Driver
      jpa:
        hibernate:
          ddl-auto: update
```

## 6. Distributed Saga Participation

This service participates in **3** distributed saga(s) coordinated by `fractalx-saga-orchestrator` running on **port 8099**.

> **How it works:** The saga orchestrator calls each participating service via **NetScope gRPC** in a defined sequence. If any step fails, the orchestrator calls compensation methods in **reverse order** to roll back changes.

### `onboard-employee-saga` — **Owner** 👑

> Onboards a new employee: sets up payroll and assigns to department.

This service **owns** this saga. The `@DistributedSaga` annotation was found in `EmployeeModule.onboardEmployee()` and the orchestrator service was auto-generated from it.

**To trigger this saga**, POST to the orchestrator instead of calling services directly:

```bash
curl -X POST http://localhost:8099/saga/onboard-employee-saga/start \
  -H "Content-Type: application/json" \
  -d '{\"firstName\":\"example\",\"lastName\":\"example\",\"email\":\"example\",\"position\":\"example\",\"salary\":99.99,\"departmentId\":1}'
```

**Payload fields** (`OnboardEmployeeSagaPayload`):

| Field | Type |
| :--- | :--- |
| `firstName` | `String` |
| `lastName` | `String` |
| `email` | `String` |
| `position` | `String` |
| `salary` | `BigDecimal` |
| `departmentId` | `Long` |

**Execution sequence managed by the orchestrator:**

1. `payroll-service` → `setupPayroll()`  ↩ compensate: `cancelSetupPayroll()`
2. `department-service` → `assignToDepartment()`  ↩ compensate: `cancelAssignToDepartment()`

> **⚠️ Important:** Do **not** call `PayrollService` or other saga participants directly in your business logic. The orchestrator now manages the full call sequence, state tracking, and rollback.

### `approve-leave-saga` — **Participant** 🔗

> Approves employee leave: marks employee on leave and deducts pay.

The saga orchestrator calls this service's methods via **NetScope gRPC on port 18081**.

| Step # | Forward Call | Compensation | Triggered When |
| :---: | :--- | :--- | :--- |
| 1 | `markOnLeave()` | `cancelMarkOnLeave()` | Step 1 of the saga |

**What to expect at runtime:**

- The orchestrator serialises the saga payload and dispatches calls over gRPC.
- Parameters are extracted from the original saga payload and matched by name.
- If this service throws an exception, the orchestrator will invoke the corresponding **compensation method** to undo the side-effect.

**Track the saga execution that called this service:**

```bash
# The correlationId is returned when the saga owner starts the saga
curl http://localhost:8099/saga/status/<correlationId>
```

### `hire-employee-saga` — **Participant** 🔗

> Hires a candidate: creates employee record and updates department headcount.

The saga orchestrator calls this service's methods via **NetScope gRPC on port 18081**.

| Step # | Forward Call | Compensation | Triggered When |
| :---: | :--- | :--- | :--- |
| 1 | `createEmployee()` | `cancelCreateEmployee()` | Step 1 of the saga |

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
