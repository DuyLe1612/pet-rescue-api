-- =========================================================
-- V22 — Drop Spring Batch tables (no @Job/@Step in codebase)
-- =========================================================
-- Cleanup: spring-boot-starter-batch was removed; the 5 batch
-- tables were auto-created by `spring.batch.jdbc.initialize-schema=always`
-- but the project never defines a Job or Step. These tables
-- are empty and unused.

DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT CASCADE;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE CASCADE;
DROP TABLE IF EXISTS BATCH_STEP_SEQ CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_SEQ CASCADE;
DROP TABLE IF EXISTS BATCH_HISTORY_SEQ CASCADE;
