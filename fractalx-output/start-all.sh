#!/bin/bash

echo "Starting all FractalX microservices..."

# Start fractalx-registry first
echo "Starting fractalx-registry on port 8761..."
cd fractalx-registry && mvn spring-boot:run > ../fractalx-registry.log 2>&1 &
cd ..
echo "Waiting 5s for registry to become ready..."
sleep 5

# Start all microservices
echo "Starting department-service on port 8085..."
cd department-service && mvn spring-boot:run > ../department-service.log 2>&1 &
cd ..

echo "Starting employee-service on port 8081..."
cd employee-service && mvn spring-boot:run > ../employee-service.log 2>&1 &
cd ..

echo "Starting leave-service on port 8083..."
cd leave-service && mvn spring-boot:run > ../leave-service.log 2>&1 &
cd ..

echo "Starting payroll-service on port 8082..."
cd payroll-service && mvn spring-boot:run > ../payroll-service.log 2>&1 &
cd ..

echo "Starting recruitment-service on port 8084..."
cd recruitment-service && mvn spring-boot:run > ../recruitment-service.log 2>&1 &
cd ..

echo "Starting Saga Orchestrator..."
cd fractalx-saga-orchestrator && mvn spring-boot:run > ../fractalx-saga-orchestrator.log 2>&1 &
cd ..

echo "All services and gateway started successfully!"
echo "Gateway URL: http://localhost:9999"
echo "To stop all services, run: ./stop-all.sh"

echo "Service URLs:"
echo "  department-service:  http://localhost:8085"
echo "  employee-service:    http://localhost:8081"
echo "  leave-service:       http://localhost:8083"
echo "  payroll-service:     http://localhost:8082"
echo "  recruitment-service: http://localhost:8084"
echo "  saga-orchestrator:   http://localhost:8099"
