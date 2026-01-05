package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CompanySubscriptionEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.SubscriptionItemEntity;
import io.hephaistos.flagforge.configuration.StripeConfiguration;
import io.hephaistos.flagforge.controller.dto.SubscriptionStatusResponse;
import io.hephaistos.flagforge.data.repository.CompanySubscriptionRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.SubscriptionItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for Stripe service implementations.
 * <p>
 * Contains shared logic for subscription status retrieval and other database-only operations that
 * are the same in both production and mock modes.
 */
@Transactional
public abstract class AbstractStripeService implements StripeService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final StripeConfiguration config;
    protected final CompanySubscriptionRepository subscriptionRepository;
    protected final SubscriptionItemRepository subscriptionItemRepository;
    protected final EnvironmentRepository environmentRepository;

    protected AbstractStripeService(StripeConfiguration config,
            CompanySubscriptionRepository subscriptionRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            EnvironmentRepository environmentRepository) {
        this.config = config;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .map(this::buildSubscriptionStatusResponse)
                .orElse(null);
    }

    protected SubscriptionStatusResponse buildSubscriptionStatusResponse(
            CompanySubscriptionEntity subscription) {
        List<SubscriptionItemEntity> items =
                subscriptionItemRepository.findBySubscription_Id(subscription.getId());

        List<SubscriptionStatusResponse.SubscriptionItemDto> itemDtos = new ArrayList<>();
        int totalPriceCents = 0;

        for (SubscriptionItemEntity item : items) {
            EnvironmentEntity env =
                    environmentRepository.findById(item.getEnvironmentId()).orElse(null);
            if (env != null) {
                int priceCents = env.getTier().getMonthlyPriceUsd() * 100;
                totalPriceCents += priceCents;
                itemDtos.add(new SubscriptionStatusResponse.SubscriptionItemDto(env.getId(),
                        env.getName(), env.getTier().name(), priceCents));
            }
        }

        return new SubscriptionStatusResponse(subscription.getStatus().name(), totalPriceCents,
                subscription.getCurrentPeriodEnd(), subscription.getCancelAtPeriodEnd(), itemDtos);
    }
}
