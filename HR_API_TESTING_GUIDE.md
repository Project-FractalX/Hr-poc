# HR System — Complete API Testing Guide

> **Read this first.** The system has data dependencies. You must create data in the order shown below.
> A Department must exist before you can onboard an Employee. An Employee must exist before you can approve Leave or hire a Candidate. Follow the sections in order.

---

## Base URL

```
http://localhost:8080
```

---

## PHASE 1 — Create Departments (no dependencies)

Departments are **auto-seeded on startup** with codes: `ENG`, `HR`, `FIN`, `MKT` (IDs 1–4).

You can skip to Phase 2, or create extra departments manually.

### ✅ 1.1 — Verify auto-seeded departments exist

```
GET /api/department
```

**Expected response — 4 departments:**
```json
[
  { "id": 1, "code": "ENG", "name": "Engineering",     "headcount": 0, "annualBudget": 5000000, "status": "ACTIVE" },
  { "id": 2, "code": "HR",  "name": "Human Resources", "headcount": 0, "annualBudget": 1500000, "status": "ACTIVE" },
  { "id": 3, "code": "FIN", "name": "Finance",          "headcount": 0, "annualBudget": 2000000, "status": "ACTIVE" },
  { "id": 4, "code": "MKT", "name": "Marketing",        "headcount": 0, "annualBudget": 1800000, "status": "ACTIVE" }
]
```

---

### ➕ 1.2 — (Optional) Create a custom department

```
POST /api/department
Content-Type: application/json
```

```json
{
  "code": "OPS",
  "name": "Operations",
  "annualBudget": 2500000
}
```

**Expected response:**
```json
{
  "id": 5,
  "code": "OPS",
  "name": "Operations",
  "headcount": 0,
  "annualBudget": 2500000,
  "status": "ACTIVE",
  "createdAt": "2026-03-06T10:00:00"
}
```

---

### 🔍 1.3 — Get a single department

```
GET /api/department/1
```

---

## PHASE 2 — Onboard Employees *(requires Phase 1)*

> **Dependency:** `departmentId` must be a valid department from Phase 1.
>
> This starts the **`onboard-employee-saga`**:
> 1. Creates Employee (status = `PENDING`)
> 2. Calls `payrollService.setupPayroll()` → creates PayrollRecord
> 3. Calls `departmentService.assignToDepartment()` → increments department headcount
> 4. On success → Employee status becomes `ACTIVE`

---

### ➕ 2.1 — Onboard Employee 1 (Engineering)

```
POST /api/employees/onboard
Content-Type: application/json
```

```json
{
  "firstName": "Alice",
  "lastName": "Wren",
  "email": "alice.wren@corp.com",
  "position": "Senior Engineer",
  "salary": 95000,
  "departmentId": 1
}
```

**Expected response:**
```json
{
  "id": 1,
  "firstName": "Alice",
  "lastName": "Wren",
  "email": "alice.wren@corp.com",
  "position": "Senior Engineer",
  "salary": 95000,
  "departmentId": 1,
  "status": "ACTIVE",
  "hireDate": "2026-03-06",
  "payrollSetUp": true,
  "createdAt": "2026-03-06T10:01:00"
}
```

---

### ➕ 2.2 — Onboard Employee 2 (HR department)

```
POST /api/employees/onboard
Content-Type: application/json
```

```json
{
  "firstName": "James",
  "lastName": "Porter",
  "email": "james.porter@corp.com",
  "position": "HR Manager",
  "salary": 72000,
  "departmentId": 2
}
```

---

### ➕ 2.3 — Onboard Employee 3 (Finance)

```
POST /api/employees/onboard
Content-Type: application/json
```

```json
{
  "firstName": "Sara",
  "lastName": "Kim",
  "email": "sara.kim@corp.com",
  "position": "Financial Analyst",
  "salary": 68000,
  "departmentId": 3
}
```

---

### 🔍 2.4 — Verify payroll was auto-created for Employee 1

> This confirms the saga step ran — `payrollService.setupPayroll()` was called.

```
GET /api/payroll/employee/1
```

**Expected response:**
```json
[
  {
    "id": 1,
    "employeeId": 1,
    "payYear": 2026,
    "payMonth": 3,
    "basicSalary": 95000,
    "deductions": 0,
    "netSalary": 95000,
    "leaveDaysDeducted": 0,
    "status": "ACTIVE"
  }
]
```

---

### 🔍 2.5 — Verify department headcount increased

> Confirms the saga step `departmentService.assignToDepartment()` ran.

```
GET /api/department/1
```

**Expected:** `"headcount": 1`

---

### 🔍 2.6 — List all active employees

```
GET /api/employees
```

---

### 🔍 2.7 — Get a specific employee

```
GET /api/employees/1
```

---

### 🔍 2.8 — Get employees by department

```
GET /api/employees/department/1
```

---

## PHASE 3 — Approve Leave *(requires Phase 2)*

> **Dependency:** `employeeId` must be a valid `ACTIVE` employee from Phase 2.
>
> This starts the **`approve-leave-saga`**:
> 1. Creates LeaveEntry (status = `PENDING`)
> 2. Calls `employeeService.markOnLeave()` → Employee status becomes `ON_LEAVE`
> 3. Calls `payrollService.deductLeave()` → Deducts salary for leave days
> 4. On success → LeaveEntry status becomes `APPROVED`

---

### ➕ 3.1 — Approve Annual Leave for Employee 1

```
POST /api/leave/approve
Content-Type: application/json
```

```json
{
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-04-07",
  "endDate": "2026-04-11",
  "leaveDays": 5,
  "reason": "Family vacation to Sri Lanka"
}
```

**Expected response:**
```json
{
  "id": 1,
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-04-07",
  "endDate": "2026-04-11",
  "leaveDays": 5,
  "reason": "Family vacation to Sri Lanka",
  "status": "APPROVED",
  "requestedAt": "2026-03-06T10:05:00"
}
```

---

### ➕ 3.2 — Approve Sick Leave for Employee 2

```
POST /api/leave/approve
Content-Type: application/json
```

```json
{
  "employeeId": 2,
  "leaveType": "SICK",
  "startDate": "2026-03-10",
  "endDate": "2026-03-12",
  "leaveDays": 3,
  "reason": "Medical appointment and recovery"
}
```

---

### 🔍 3.3 — Verify payroll deduction was applied for Employee 1

> Confirms `payrollService.deductLeave()` saga step ran.
> Daily rate = 95000 ÷ 22 = 4318.18. For 5 days: deduction = 21590.90

```
GET /api/payroll/employee/1
```

**Expected:**
```json
[
  {
    "employeeId": 1,
    "basicSalary": 95000,
    "deductions": 21590.90,
    "netSalary": 73409.10,
    "leaveDaysDeducted": 5,
    "status": "ACTIVE"
  }
]
```

---

### 🔍 3.4 — Get a specific leave entry

```
GET /api/leave/1
```

---

### 🔍 3.5 — Get all leave entries for an employee

```
GET /api/leave/employee/1
```

---

### ➕ 3.6 — Submit a leave request that you will then reject

```
POST /api/leave/approve
Content-Type: application/json
```

```json
{
  "employeeId": 3,
  "leaveType": "UNPAID",
  "startDate": "2026-05-01",
  "endDate": "2026-05-15",
  "leaveDays": 11,
  "reason": "Personal travel"
}
```

> Note the `id` in the response — use it in the next step.

---

### ❌ 3.7 — Reject the leave request (use the ID from 3.6)

```
PATCH /api/leave/3/reject
```

*(no body)*

**Expected:** HTTP `204 No Content`

Verify it was rejected:
```
GET /api/leave/3
```
**Expected:** `"status": "REJECTED"`

---

## PHASE 4 — Recruitment & Hiring *(requires Phase 1)*

> **Dependency:** `departmentId` must be valid from Phase 1.
> The **hire step** also needs Employee and Department to be writable (active).
>
> This phase has 3 sub-steps: Apply → Shortlist → Hire
>
> **Hire** starts the **`hire-employee-saga`**:
> 1. Marks Candidate as `PENDING`
> 2. Calls `employeeService.createEmployee()` → creates a new Employee record
> 3. Calls `departmentService.incrementHeadcount()` → bumps department headcount
> 4. On success → Candidate status becomes `HIRED`

---

### ➕ 4.1 — Candidate applies for a job

```
POST /api/recruitment/apply
Content-Type: application/json
```

```json
{
  "firstName": "Bob",
  "lastName": "Lane",
  "email": "bob.lane@gmail.com",
  "position": "QA Lead",
  "departmentId": 1,
  "expectedSalary": 75000
}
```

**Expected response:**
```json
{
  "id": 1,
  "firstName": "Bob",
  "lastName": "Lane",
  "email": "bob.lane@gmail.com",
  "appliedPosition": "QA Lead",
  "targetDepartmentId": 1,
  "offeredSalary": 75000,
  "applicationStatus": "APPLIED",
  "appliedAt": "2026-03-06T10:10:00"
}
```

---

### ➕ 4.2 — Second candidate applies

```
POST /api/recruitment/apply
Content-Type: application/json
```

```json
{
  "firstName": "Priya",
  "lastName": "Nair",
  "email": "priya.nair@gmail.com",
  "position": "DevOps Engineer",
  "departmentId": 1,
  "expectedSalary": 88000
}
```

---

### ✅ 4.3 — Shortlist Candidate 1

```
PATCH /api/recruitment/1/shortlist
```

*(no body)*

**Expected response:**
```json
{
  "id": 1,
  "applicationStatus": "SHORTLISTED"
}
```

---

### ✅ 4.4 — Shortlist Candidate 2

```
PATCH /api/recruitment/2/shortlist
```

*(no body)*

---

### 🔍 4.5 — View all shortlisted candidates

```
GET /api/recruitment/shortlisted
```

---

### ❌ 4.6 — Reject a candidate

> If you had a 3rd candidate you don't want to hire:

```
POST /api/recruitment/apply
Content-Type: application/json
```

```json
{
  "firstName": "Dan",
  "lastName": "Cruz",
  "email": "dan.cruz@gmail.com",
  "position": "Intern",
  "departmentId": 4,
  "expectedSalary": 25000
}
```

Then reject them:

```
PATCH /api/recruitment/3/reject
```

*(no body)*

---

### 🚀 4.7 — Hire Candidate 1 (starts `hire-employee-saga`)

> The `email` here is the **official work email** — it can be different from the application email.

```
POST /api/recruitment/hire
Content-Type: application/json
```

```json
{
  "candidateId": 1,
  "firstName": "Bob",
  "lastName": "Lane",
  "email": "bob.lane@corp.com",
  "position": "QA Lead",
  "salary": 78000,
  "departmentId": 1
}
```

**Expected response:**
```json
{
  "id": 1,
  "firstName": "Bob",
  "lastName": "Lane",
  "email": "bob.lane@gmail.com",
  "applicationStatus": "HIRED",
  "offeredSalary": 78000,
  "targetDepartmentId": 1
}
```

---

### 🚀 4.8 — Hire Candidate 2

```
POST /api/recruitment/hire
Content-Type: application/json
```

```json
{
  "candidateId": 2,
  "firstName": "Priya",
  "lastName": "Nair",
  "email": "priya.nair@corp.com",
  "position": "DevOps Engineer",
  "salary": 90000,
  "departmentId": 1
}
```

---

### 🔍 4.9 — Verify new employees were created by the saga

```
GET /api/employees
```

**You should now see 5 employees** — 3 from Phase 2 + 2 hired in Phase 4.

---

### 🔍 4.10 — Verify department headcount updated

```
GET /api/department/1
```

**Expected:** `"headcount": 4` (1 from Phase 2 onboard + 1 from hire saga ×2 + 1 from the other onboard that mapped to dept 1)

> Exact number depends on how many employees you assigned to department 1.

---

## PHASE 5 — Payroll Checks

---

### 🔍 5.1 — Get payroll for a specific employee

```
GET /api/payroll/employee/1
```

---

### 🔍 5.2 — Get all payroll records for current month

```
GET /api/payroll/period/2026/3
```

---

## PHASE 6 — Termination

> **Dependency:** Employee must be `ACTIVE`.

---

### ❌ 6.1 — Terminate an employee

```
DELETE /api/employees/3/terminate
```

*(no body)*

**Expected:** HTTP `204 No Content`

Verify:
```
GET /api/employees/3
```

**Expected:** `"status": "TERMINATED"`

---

## Full Dependency Map

```
Phase 1 — Department
    │
    ├── Phase 2 — Onboard Employee (onboard-employee-saga)
    │       │
    │       │  saga creates ──► PayrollRecord (auto)
    │       │  saga updates ──► Department.headcount (auto)
    │       │
    │       ├── Phase 3 — Approve Leave (approve-leave-saga)
    │       │       │
    │       │       │  saga updates ──► Employee.status = ON_LEAVE
    │       │       └── saga updates ──► PayrollRecord.deductions
    │       │
    │       └── Phase 6 — Terminate Employee
    │
    └── Phase 4 — Recruitment (hire-employee-saga)
            │
            ├── Apply    (no deps — just department)
            ├── Shortlist (requires candidate)
            ├── Reject    (requires candidate)
            └── Hire     (starts saga)
                    │
                    ├── saga creates ──► Employee (new record)
                    └── saga updates ──► Department.headcount
```

---

## Leave Type Reference

| Value | Meaning |
|---|---|
| `ANNUAL` | Paid annual leave |
| `SICK` | Sick leave |
| `MATERNITY` | Maternity leave |
| `PATERNITY` | Paternity leave |
| `UNPAID` | Unpaid leave |

---

## Employee Status Reference

| Status | When set |
|---|---|
| `PENDING` | Saga just started — payroll/dept not yet confirmed |
| `ACTIVE` | Saga completed successfully |
| `ON_LEAVE` | After leave saga step 1 runs |
| `TERMINATED` | After DELETE /terminate |
| `CANCELLED` | Saga failed — compensation ran |

---

## Candidate Status Reference

| Status | When set |
|---|---|
| `APPLIED` | After POST /apply |
| `SHORTLISTED` | After PATCH /shortlist |
| `PENDING` | Hire saga just started |
| `HIRED` | Hire saga completed successfully |
| `REJECTED` | After PATCH /reject |
| `CANCELLED` | Hire saga failed — compensation ran |

---

## Quick Copy-Paste Curl Block (run in order)

```bash
BASE=http://localhost:8080

# Phase 1 — Verify departments
curl -s $BASE/api/department | jq .

# Phase 2 — Onboard 3 employees
curl -s -X POST $BASE/api/employees/onboard \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Wren","email":"alice.wren@corp.com","position":"Senior Engineer","salary":95000,"departmentId":1}' | jq .

curl -s -X POST $BASE/api/employees/onboard \
  -H "Content-Type: application/json" \
  -d '{"firstName":"James","lastName":"Porter","email":"james.porter@corp.com","position":"HR Manager","salary":72000,"departmentId":2}' | jq .

curl -s -X POST $BASE/api/employees/onboard \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Sara","lastName":"Kim","email":"sara.kim@corp.com","position":"Financial Analyst","salary":68000,"departmentId":3}' | jq .

# Verify payroll was created and headcount bumped
curl -s $BASE/api/payroll/employee/1 | jq .
curl -s $BASE/api/department/1 | jq .

# Phase 3 — Approve leave
curl -s -X POST $BASE/api/leave/approve \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"leaveType":"ANNUAL","startDate":"2026-04-07","endDate":"2026-04-11","leaveDays":5,"reason":"Vacation"}' | jq .

# Verify deduction
curl -s $BASE/api/payroll/employee/1 | jq .

# Phase 4 — Recruitment pipeline
curl -s -X POST $BASE/api/recruitment/apply \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Lane","email":"bob.lane@gmail.com","position":"QA Lead","departmentId":1,"expectedSalary":75000}' | jq .

curl -s -X PATCH $BASE/api/recruitment/1/shortlist | jq .

curl -s -X POST $BASE/api/recruitment/hire \
  -H "Content-Type: application/json" \
  -d '{"candidateId":1,"firstName":"Bob","lastName":"Lane","email":"bob.lane@corp.com","position":"QA Lead","salary":78000,"departmentId":1}' | jq .

# Final checks
curl -s $BASE/api/employees | jq .
curl -s $BASE/api/department/1 | jq .
curl -s $BASE/api/payroll/period/2026/3 | jq .
```

> Remove `| jq .` if you don't have `jq` installed.
