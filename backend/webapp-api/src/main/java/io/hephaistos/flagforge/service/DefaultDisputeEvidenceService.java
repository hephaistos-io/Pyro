package io.hephaistos.flagforge.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Dispute;
import com.stripe.param.DisputeUpdateParams;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import io.hephaistos.flagforge.data.repository.UsageDailyStatisticsRepository;
import io.hephaistos.flagforge.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of DisputeEvidenceService.
 * <p>
 * Collects evidence from the application database and submits it to Stripe for dispute resolution.
 */
@Service
@Transactional(readOnly = true)
public class DefaultDisputeEvidenceService implements DisputeEvidenceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultDisputeEvidenceService.class);
    private static final String SERVICE_DOC_URL = "https://docs.flagforge.io/terms-of-service";
    private static final int USAGE_DAYS_TO_INCLUDE = 30;

    private final CompanyRepository companyRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final EnvironmentRepository environmentRepository;
    private final UsageDailyStatisticsRepository usageRepository;

    public DefaultDisputeEvidenceService(CompanyRepository companyRepository,
            CompanySubscriptionRepository subscriptionRepository,
            CustomerRepository customerRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            EnvironmentRepository environmentRepository,
            UsageDailyStatisticsRepository usageRepository) {
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.customerRepository = customerRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.environmentRepository = environmentRepository;
        this.usageRepository = usageRepository;
    }

    @Override
    public DisputeEvidence collectEvidence(UUID companyId, String chargeId) {
        LOGGER.info("Collecting dispute evidence for company {} and charge {}", companyId,
                chargeId);

        // Get customer information
        String customerEmail = "";
        String customerName = "";

        List<CustomerEntity> customers = customerRepository.findByCompanyId(companyId);
        if (!customers.isEmpty()) {
            CustomerEntity primaryCustomer = customers.getFirst();
            customerEmail = primaryCustomer.getEmail();
            customerName = primaryCustomer.getFirstName() + " " + primaryCustomer.getLastName();
        }

        // Get company name for context
        String companyName = companyRepository.findById(companyId)
                .map(CompanyEntity::getName)
                .orElse("Unknown Company");

        // Build product description from subscribed environments
        String productDescription = buildProductDescription(companyId, companyName);

        // Build usage logs
        String usageLogs = buildUsageLogs(companyId);

        LOGGER.info("Evidence collected for company {}: customer={}, environments found", companyId,
                customerEmail);

        return new DisputeEvidence(customerEmail, customerName, productDescription, usageLogs,
                SERVICE_DOC_URL);
    }

    @Override
    public void submitEvidence(String disputeId, DisputeEvidence evidence) {
        LOGGER.info("Submitting evidence for dispute {}", disputeId);

        try {
            Dispute dispute = Dispute.retrieve(disputeId);

            DisputeUpdateParams.Evidence evidenceParams = DisputeUpdateParams.Evidence.builder()
                    .setCustomerEmailAddress(evidence.customerEmail())
                    .setCustomerName(evidence.customerName())
                    .setProductDescription(evidence.productDescription())
                    .setServiceDocumentation(evidence.usageLogs())
                    .build();

            DisputeUpdateParams params = DisputeUpdateParams.builder()
                    .setEvidence(evidenceParams)
                    .setSubmit(true)
                    .build();

            dispute.update(params);
            LOGGER.info("Evidence submitted successfully for dispute {}", disputeId);

        }
        catch (StripeException e) {
            LOGGER.error("Failed to submit evidence for dispute {}", disputeId, e);
            throw new PaymentException("Failed to submit dispute evidence", e);
        }
    }

    private String buildProductDescription(UUID companyId, String companyName) {
        Optional<CompanySubscriptionEntity> subscriptionOpt =
                subscriptionRepository.findByCompanyId(companyId);

        if (subscriptionOpt.isEmpty()) {
            return String.format(
                    "FlagForge feature flag management service for %s. " + "Subscription-based SaaS service providing real-time feature flag management, " + "A/B testing, and deployment control capabilities.",
                    companyName);
        }

        CompanySubscriptionEntity subscription = subscriptionOpt.get();
        List<SubscriptionItemEntity> items =
                subscriptionItemRepository.findBySubscription_Id(subscription.getId());

        if (items.isEmpty()) {
            return String.format(
                    "FlagForge feature flag management service for %s. " + "Subscription-based SaaS service providing real-time feature flag management.",
                    companyName);
        }

        StringBuilder description = new StringBuilder();
        description.append(String.format(
                "FlagForge feature flag management service for %s.\n\n" + "Subscribed Environments:\n",
                companyName));

        for (SubscriptionItemEntity item : items) {
            environmentRepository.findById(item.getEnvironmentId()).ifPresent(env -> {
                description.append(String.format(
                        "- %s (%s tier): Real-time feature flag management, " + "%d requests/month limit, API access, dashboard access\n",
                        env.getName(), env.getTier().name(), env.getTier().getRequestsPerMonth()));
            });
        }

        description.append(
                "\nService includes: Feature flag management, A/B testing, " + "gradual rollouts, targeting rules, analytics dashboard, and 24/7 API availability.");

        return description.toString();
    }

    private String buildUsageLogs(UUID companyId) {
        Optional<CompanySubscriptionEntity> subscriptionOpt =
                subscriptionRepository.findByCompanyId(companyId);

        if (subscriptionOpt.isEmpty()) {
            return "No usage data available.";
        }

        CompanySubscriptionEntity subscription = subscriptionOpt.get();
        List<SubscriptionItemEntity> items =
                subscriptionItemRepository.findBySubscription_Id(subscription.getId());

        if (items.isEmpty()) {
            return "No active environments found.";
        }

        LocalDate startDate = LocalDate.now().minusDays(USAGE_DAYS_TO_INCLUDE);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder logs = new StringBuilder();
        logs.append("SERVICE USAGE LOGS\n");
        logs.append("==================\n");
        logs.append(String.format("Period: %s to %s\n\n", startDate.format(formatter),
                LocalDate.now().format(formatter)));

        for (SubscriptionItemEntity item : items) {
            Optional<EnvironmentEntity> envOpt =
                    environmentRepository.findById(item.getEnvironmentId());
            if (envOpt.isEmpty())
                continue;

            EnvironmentEntity env = envOpt.get();
            logs.append(String.format("Environment: %s (%s tier)\n", env.getName(),
                    env.getTier().name()));
            logs.append("-".repeat(40)).append("\n");

            List<UsageDailyStatisticsEntity> usage =
                    usageRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                            env.getId(), startDate);

            if (usage.isEmpty()) {
                logs.append("No API requests recorded for this period.\n\n");
            }
            else {
                long totalRequests = usage.stream()
                        .mapToLong(UsageDailyStatisticsEntity::getTotalRequests)
                        .sum();

                logs.append(String.format("Total API Requests: %,d\n", totalRequests));
                logs.append(String.format("Days with Activity: %d\n\n", usage.size()));

                logs.append("Daily Breakdown:\n");
                for (UsageDailyStatisticsEntity stat : usage) {
                    logs.append(String.format("  %s: %,d requests (peak: %d req/s)\n",
                            stat.getDate().format(formatter), stat.getTotalRequests(),
                            stat.getPeakRequestsPerSecond()));
                }
                logs.append("\n");
            }
        }

        logs.append(
                "This data confirms active service usage by the customer " + "during the billing period. All API access is logged and authenticated.\n");

        return logs.toString();
    }
}
