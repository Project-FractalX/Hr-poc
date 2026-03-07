package org.fractalx.admin.data;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Baked-in registry of saga orchestration definitions derived at generation time.
 * Reflects modules that have cross-service dependencies and may participate in sagas.
 */
@Component
public class SagaMetaRegistry {

    public record SagaInfo(
            String sagaId, String orchestratedBy,
            List<String> steps, List<String> compensationSteps, boolean enabled) {}

    private static final List<SagaInfo> SAGAS = List.of(
        new SagaInfo("onboard-employee-saga", "employee-service", List.of("payroll-service:setupPayroll", "department-service:assignToDepartment"), List.of("payroll-service:cancelSetupPayroll", "department-service:cancelAssignToDepartment"), true),
        new SagaInfo("approve-leave-saga", "leave-service", List.of("employee-service:markOnLeave", "payroll-service:deductLeave"), List.of("employee-service:cancelMarkOnLeave", "payroll-service:cancelDeductLeave"), true),
        new SagaInfo("hire-employee-saga", "recruitment-service", List.of("employee-service:createEmployee", "department-service:incrementHeadcount"), List.of("employee-service:cancelCreateEmployee", "department-service:cancelIncrementHeadcount"), true)
    );

    public List<SagaInfo> getAll()                     { return SAGAS; }
    public int            count()                      { return SAGAS.size(); }

    public Optional<SagaInfo> findById(String sagaId) {
        return SAGAS.stream().filter(s -> s.sagaId().equals(sagaId)).findFirst();
    }

    public boolean hasSagas() { return !SAGAS.isEmpty(); }
}
