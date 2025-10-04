package com.pig4cloud.pig.smm.job;

import com.pig4cloud.pig.daemon.quartz.constants.PigQuartzEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A simple job that intentionally throws an exception to verify
 * that stack traces are dumped to the console/logs.
 */
@Slf4j
@Component
public class TestExceptionJob {

	/**
	 * Execute method signature aligns with other jobs in this module.
	 * It will always throw a RuntimeException to test console dumping.
	 */
	public String execute() {
		log.info("TestExceptionJob starting - this will deliberately throw an exception");
		try {
			simulateFailure();
			// unreachable, here for API symmetry
			return PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}
		catch (RuntimeException e) {
			// Log the exception with stack trace; this should print to console
			log.error("TestExceptionJob failed as expected", e);
			// Rethrow so any caller/scheduler also logs the stack trace
			throw e;
		}
	}

	private void simulateFailure() {
		try {
			explode();
		}
		catch (IllegalStateException cause) {
			throw new RuntimeException("Wrapped exception from TestExceptionJob", cause);
		}
	}

	private void explode() {
		throw new IllegalStateException("Intentional test failure in TestExceptionJob");
	}
}
