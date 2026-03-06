# HR Modular Monolith — FractalX Ready

A complete HR system written as a FractalX-decomposable modular monolith.
Run it today as a single Spring Boot app. Decompose it tomorrow into microservices
with `mvn fractalx:decompose`.

---

## Modules at a Glance

| Module | Port | Owns | Role |
|---|---|---|---|
| `employee-service` | 8081 | `employees` | Saga owner (onboard) |
| `payroll-service`  | 8082 | `payroll_records` | Leaf — called by others |
| `leave-service`    | 8083 | `leave_entries` | Saga owner (approve leave) |
| `recruitment-service` | 8084 | `candidates` | Saga owner (hire) |
| `department-service`  | 8085 | `departments` | Leaf — called by others |

---

## Cross-Module Calls (6 total)

```
EmployeeModule   → PayrollService.setupPayroll()          (onboard saga step 1)
EmployeeModule   → DepartmentService.assignToDepartment() (onboard saga step 2)
LeaveModule      → EmployeeService.markOnLeave()          (leave saga step 1)
LeaveModule      → PayrollService.deductLeave()           (leave saga step 2)
RecruitmentModule → EmployeeService.createEmployee()      (hire saga step 1)
RecruitmentModule → DepartmentService.incrementHeadcount()(hire saga step 2)
```

---

## Distributed Sagas (3 total)

### 1. `onboard-employee-saga` — owned by `employee-service`

```
onboardEmployee(firstName, lastName, email, position, salary, departmentId)
  │
  ├── [local]  Employee(status=PENDING) saved
  ├── [cross]  PayrollService.setupPayroll(employeeId, salary)
  │              compensation: cancelSetupPayroll()
  ├── [cross]  DepartmentService.assignToDepartment(employeeId, departmentId)
  │              compensation: cancelAssignToDepartment()
  │
  ├── [success] → employee.status = ACTIVE, payrollSetUp = true
  └── [failure] → cancelOnboardEmployee() → employee.status = CANCELLED
```

### 2. `approve-leave-saga` — owned by `leave-service`

```
approveLeave(employeeId, leaveType, startDate, endDate, leaveDays, reason)
  │
  ├── [local]  LeaveEntry(status=PENDING) saved
  ├── [cross]  EmployeeService.markOnLeave(employeeId)
  │              compensation: cancelMarkOnLeave()
  ├── [cross]  PayrollService.deductLeave(employeeId, leaveDays)
  │              compensation: cancelDeductLeave()
  │
  ├── [success] → leaveEntry.status = APPROVED, employee.status = ON_LEAVE
  └── [failure] → cancelApproveLeave() → leaveEntry.status = CANCELLED
```

### 3. `hire-employee-saga` — owned by `recruitment-service`

```
hireEmployee(candidateId, firstName, lastName, email, position, salary, departmentId)
  │
  ├── [local]  Candidate(status=PENDING) saved
  ├── [cross]  EmployeeService.createEmployee(firstName, lastName, email, position, salary, deptId)
  │              compensation: cancelCreateEmployee()
  ├── [cross]  DepartmentService.incrementHeadcount(departmentId)
  │              compensation: cancelIncrementHeadcount()
  │
  ├── [success] → candidate.status = HIRED, new Employee created
  └── [failure] → cancelHireEmployee() → candidate.status = CANCELLED
```

---

## Running (Monolith mode)

```bash
mvn spring-boot:run
```

- App: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console  (JDBC URL: `jdbc:h2:mem:hr_db`)

---

## Quick API Walkthrough

```bash
# 1. List seed departments (auto-created on startup)
curl http://localhost:8080/api/department

# 2. Onboard an employee (starts onboard-employee-saga)
curl -X POST http://localhost:8080/api/employees/onboard \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Wren","email":"alice@corp.com",
       "position":"Senior Engineer","salary":95000,"departmentId":1}'

# 3. Approve leave (starts approve-leave-saga)
curl -X POST http://localhost:8080/api/leave/approve \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"leaveType":"ANNUAL","startDate":"2026-04-07",
       "endDate":"2026-04-11","leaveDays":5,"reason":"Family vacation"}'

# 4. Apply for a job
curl -X POST http://localhost:8080/api/recruitment/apply \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Lane","email":"bob@email.com",
       "position":"QA Lead","departmentId":1,"expectedSalary":75000}'

# 5. Shortlist the candidate
curl -X PATCH http://localhost:8080/api/recruitment/1/shortlist

# 6. Hire the candidate (starts hire-employee-saga)
curl -X POST http://localhost:8080/api/recruitment/hire \
  -H "Content-Type: application/json" \
  -d '{"candidateId":1,"firstName":"Bob","lastName":"Lane",
       "email":"bob.lane@corp.com","position":"QA Lead",
       "salary":78000,"departmentId":1}'
```

---

## Decompose to Microservices

```bash
# Generates 10 services: 5 business + gateway + registry + saga-orchestrator + logger + admin
mvn fractalx:decompose

cd fractalx-output
./start-all.sh          # OR: docker-compose up -d
```

Generated services are independent Spring Boot apps each with their own H2 (dev)
or configurable RDBMS (prod). All cross-module calls become gRPC via NetScope.
All sagas run via the saga orchestrator using the transactional outbox pattern.

---

## FractalX Rules Compliance

| Rule | Status |
|---|---|
| One `@DecomposableModule` per module | ✓ (5 modules, 5 boundary classes) |
| No `*Service` same-module fields in boundary class | ✓ verified |
| No cross-module `@ManyToOne` / `@OneToMany` | ✓ verified |
| All saga params are standard Java types | ✓ (`Long`, `String`, `BigDecimal`, `Integer`) |
| Compensation methods follow `cancel` + CapitalisedName | ✓ (9 compensation methods) |
| `compensationMethod` in same class as saga method | ✓ all 3 sagas |
| Entity `status` field for saga-owned entities | ✓ all 5 entities |
| Controller paths use `/api/<service-base>/` | ✓ all 5 controllers |
