package cl.textilcircular.api.feature.metrics;

import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.MetricCount;
import cl.textilcircular.api.core.repository.MetricsQueries;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.metrics.MetricsDtos.Kpi;
import cl.textilcircular.api.feature.metrics.MetricsDtos.MetricValue;
import cl.textilcircular.api.feature.metrics.MetricsDtos.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** El panel del administrador: conteos reales de cuentas, conversaciones, emprendimientos y analítica. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class MetricsService {

    private static final int THIRTY_DAYS = 30;
    private static final int NINETY_DAYS = 90;

    private final UserAccountRepository accounts;
    private final MetricsQueries queries;
    private final Clock clock;

    Metrics get(Caller caller, Integer days, Long regionId, Long serviceId) {
        requireAdministrator(caller);

        Instant to = clock.instant();
        boolean windowed = days != null;
        Instant from;
        Instant previousFrom;
        if (windowed) {
            if (days != THIRTY_DAYS && days != NINETY_DAYS) {
                throw new ValidationException("days", "La ventana debe ser de 30 o 90 días");
            }
            from = to.minus(Duration.ofDays(days));
            previousFrom = from.minus(Duration.ofDays(days));
        } else {
            from = Instant.EPOCH;
            previousFrom = Instant.EPOCH;
        }

        long searches = queries.searches(from, to, regionId, serviceId);
        long profileViews = queries.profileViews(from, to, regionId, serviceId);
        long conversations = queries.conversations(from, to, regionId, serviceId);
        long registrations = queries.registrations(from, to);

        return new Metrics(
                queries.activeEntrepreneurs(regionId, serviceId),
                queries.newEntrepreneurs(from, to, regionId, serviceId),
                new Kpi(searches, previous(windowed, previousFrom, from, regionId, serviceId,
                        queries::searches)),
                new Kpi(profileViews, previous(windowed, previousFrom, from, regionId, serviceId,
                        queries::profileViews)),
                new Kpi(conversations, previous(windowed, previousFrom, from, regionId, serviceId,
                        queries::conversations)),
                queries.effectiveConversations(from, to, regionId, serviceId),
                new Kpi(registrations, windowed ? queries.registrations(previousFrom, from) : 0),
                values(queries.byRegion(regionId, serviceId)),
                values(queries.byService(from, to, regionId, serviceId)),
                values(queries.byAgeRange(regionId, serviceId)),
                values(queries.mostVisitedBusinesses(from, to, regionId, serviceId)));
    }

    @FunctionalInterface
    private interface WindowedQuery {
        long count(Instant from, Instant to, Long regionId, Long serviceId);
    }

    /** Las métricas de todo el período no tienen ventana anterior, así que el contrato fija su valor anterior en 0. */
    private static long previous(boolean windowed, Instant previousFrom, Instant previousTo,
                                 Long regionId, Long serviceId, WindowedQuery query) {
        return windowed ? query.count(previousFrom, previousTo, regionId, serviceId) : 0;
    }

    private static List<MetricValue> values(List<MetricCount> counts) {
        return counts.stream().map(count -> new MetricValue(count.label(), count.value())).toList();
    }

    private void requireAdministrator(Caller caller) {
        UserAccount account = accounts.findById(caller.userId())
                .filter(a -> a.getStatus() != AccountStatus.DELETED)
                .orElseThrow(UnauthorizedException::new);
        account.ensureActive();
        if (!account.isAdministrator()) {
            throw new ForbiddenException();
        }
    }
}
