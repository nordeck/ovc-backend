truncate batch_job_execution_context CASCADE;
truncate batch_step_execution_context CASCADE;
truncate batch_job_execution_params CASCADE;
truncate batch_step_execution CASCADE;
truncate batch_job_execution CASCADE;
truncate batch_job_instance CASCADE;
ALTER SEQUENCE batch_job_execution_seq RESTART;
ALTER SEQUENCE batch_job_seq RESTART;
ALTER SEQUENCE batch_step_execution_seq RESTART;