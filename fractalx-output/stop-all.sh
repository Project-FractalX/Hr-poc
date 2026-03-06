#!/bin/bash

echo "Stopping all FractalX microservices..."

echo "Stopping API Gateway..."
pkill -f "fractalx-gateway"
sleep 2

echo "Stopping Saga Orchestrator..."
pkill -f "fractalx-saga-orchestrator" || true
sleep 1

echo "Stopping all services..."
pkill -f "spring-boot:run"

echo "All services stopped."
