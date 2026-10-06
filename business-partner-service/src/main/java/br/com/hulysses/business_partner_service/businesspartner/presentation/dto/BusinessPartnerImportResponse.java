package br.com.hulysses.business_partner_service.businesspartner.presentation.dto;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

public record BusinessPartnerImportResponse(Long jobExecutionId, String status, long readCount,
                                            long writeCount, long filterCount, long commitCount) {
    public static BusinessPartnerImportResponse from(JobExecution execution) {
        var steps = execution.getStepExecutions();
        return new BusinessPartnerImportResponse(execution.getId(), execution.getStatus().name(),
                steps.stream().mapToLong(StepExecution::getReadCount).sum(),
                steps.stream().mapToLong(StepExecution::getWriteCount).sum(),
                steps.stream().mapToLong(StepExecution::getFilterCount).sum(),
                steps.stream().mapToLong(StepExecution::getCommitCount).sum());
    }
}
