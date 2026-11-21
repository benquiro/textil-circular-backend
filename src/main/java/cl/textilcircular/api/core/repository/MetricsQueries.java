package cl.textilcircular.api.core.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Agregaciones de solo lectura detrás de {@code GET /admin/metrics}. SQL nativo porque el panel
 * une cuentas, negocios, conversaciones y eventos de analítica; las etiquetas son los nombres que
 * muestra la aplicación. Un filtro nulo viaja como {@code -1}, que ningún id de referencia usa.
 */
@Repository
public class MetricsQueries {

    private static final long NO_FILTER = -1L;

    private final EntityManager em;

    public MetricsQueries(EntityManager em) {
        this.em = em;
    }

    /** Emprendedores ACTIVE con un negocio no eliminado; es un conteo puntual, así que la ventana no aplica. */
    public long activeEntrepreneurs(Long regionId, Long serviceId) {
        return number("""
                select count(distinct u.user_id)
                from user_account u
                join entrepreneur e on e.user_id = u.user_id
                join business b on b.user_id = u.user_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where u.role = 'ENTREPRENEUR'
                  and u.status = 'ACTIVE'
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                """, regionId, serviceId);
    }

    /** Cuentas ENTREPRENEUR registradas en {@code [from, to)}. */
    public long newEntrepreneurs(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select count(*)
                from user_account u
                where u.role = 'ENTREPRENEUR'
                  and u.created_at >= :start and u.created_at < :to
                  and (:regionId = -1 or exists (
                      select 1 from business b join commune cm on cm.commune_id = b.commune_id
                      where b.user_id = u.user_id and b.deleted_at is null and cm.region_id = :regionId))
                  and (:serviceId = -1 or exists (
                      select 1 from business b join business_service bs on bs.business_id = b.business_id
                      where b.user_id = u.user_id and b.deleted_at is null and bs.service_id = :serviceId))
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    public long searches(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select count(*)
                from analytics_event a
                where a.type = 'DIRECTORY_SEARCH'
                  and a.occurred_at >= :start and a.occurred_at < :to
                  and (:regionId = -1 or a.region_id = :regionId)
                  and (:serviceId = -1 or a.service_id = :serviceId)
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    public long profileViews(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select count(*)
                from analytics_event a
                join business b on b.business_id = a.business_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where a.type = 'BUSINESS_VIEW'
                  and a.occurred_at >= :start and a.occurred_at < :to
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    /** Conversaciones cuya última actividad (último mensaje o creación) cae dentro de la ventana. */
    public long conversations(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select count(*)
                from (
                    select conv.conversation_id, conv.business_id,
                           coalesce(lm.last_message_at, conv.created_at) as last_activity
                    from conversation conv
                    left join lateral (
                        select max(m.sent_at) as last_message_at
                        from message m
                        where m.conversation_id = conv.conversation_id
                    ) lm on true
                ) t
                join business b on b.business_id = t.business_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where t.last_activity >= :start and t.last_activity < :to
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    /** Las conversaciones de la ventana que recibieron una respuesta de alguien distinto de su iniciador. */
    public long effectiveConversations(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select count(*)
                from (
                    select conv.conversation_id, conv.initiator_id, conv.business_id,
                           coalesce(lm.last_message_at, conv.created_at) as last_activity
                    from conversation conv
                    left join lateral (
                        select max(m.sent_at) as last_message_at
                        from message m
                        where m.conversation_id = conv.conversation_id
                    ) lm on true
                ) t
                join business b on b.business_id = t.business_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where t.last_activity >= :start and t.last_activity < :to
                  and exists (
                      select 1 from message answer
                      where answer.conversation_id = t.conversation_id
                        and answer.sender_id <> t.initiator_id)
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    /** Toda cuenta registrada en la ventana excepto los administradores; sin filtro de región ni servicio. */
    public long registrations(Instant from, Instant to) {
        Query query = em.createNativeQuery("""
                select count(*)
                from user_account u
                where u.role <> 'ADMINISTRATOR'
                  and u.created_at >= :start and u.created_at < :to
                """);
        bindWindow(query, from, to);
        return number(query);
    }

    public List<MetricCount> byRegion(Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select r.name as label, count(distinct u.user_id) as value
                from user_account u
                join entrepreneur e on e.user_id = u.user_id
                join business b on b.user_id = u.user_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                join region r on r.region_id = cm.region_id
                where u.role = 'ENTREPRENEUR'
                  and u.status = 'ACTIVE'
                  and (:regionId = -1 or r.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                group by r.region_id, r.name
                order by count(distinct u.user_id) desc, r.name
                """);
        bindFilters(query, regionId, serviceId);
        return counts(query);
    }

    public List<MetricCount> byService(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select s.name as label, count(*) as value
                from (
                    select conv.conversation_id, conv.business_id,
                           coalesce(lm.last_message_at, conv.created_at) as last_activity
                    from conversation conv
                    left join lateral (
                        select max(m.sent_at) as last_message_at
                        from message m
                        where m.conversation_id = conv.conversation_id
                    ) lm on true
                ) t
                join business b on b.business_id = t.business_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                join business_service bs on bs.business_id = b.business_id
                join service s on s.service_id = bs.service_id
                where t.last_activity >= :start and t.last_activity < :to
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or bs.service_id = :serviceId)
                group by s.service_id, s.name
                order by count(*) desc, s.name
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return counts(query);
    }

    public List<MetricCount> byAgeRange(Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select ar.description as label, count(distinct u.user_id) as value
                from user_account u
                join entrepreneur e on e.user_id = u.user_id
                join age_range ar on ar.age_range_id = e.age_range_id
                join business b on b.user_id = u.user_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where u.role = 'ENTREPRENEUR'
                  and u.status = 'ACTIVE'
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                group by ar.age_range_id, ar.description, ar.sort_order
                order by ar.sort_order
                """);
        bindFilters(query, regionId, serviceId);
        return counts(query);
    }

    public List<MetricCount> mostVisitedBusinesses(Instant from, Instant to, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery("""
                select b.name as label, count(*) as value
                from analytics_event a
                join business b on b.business_id = a.business_id and b.deleted_at is null
                join commune cm on cm.commune_id = b.commune_id
                where a.type = 'BUSINESS_VIEW'
                  and a.occurred_at >= :start and a.occurred_at < :to
                  and (:regionId = -1 or cm.region_id = :regionId)
                  and (:serviceId = -1 or exists (
                      select 1 from business_service bs
                      where bs.business_id = b.business_id and bs.service_id = :serviceId))
                group by b.business_id, b.name
                order by count(*) desc, b.name
                limit 5
                """);
        bindWindow(query, from, to);
        bindFilters(query, regionId, serviceId);
        return counts(query);
    }

    private long number(String sql, Long regionId, Long serviceId) {
        Query query = em.createNativeQuery(sql);
        bindFilters(query, regionId, serviceId);
        return number(query);
    }

    private static void bindWindow(Query query, Instant start, Instant to) {
        query.setParameter("start", start);
        query.setParameter("to", to);
    }

    private static void bindFilters(Query query, Long regionId, Long serviceId) {
        query.setParameter("regionId", regionId == null ? NO_FILTER : regionId);
        query.setParameter("serviceId", serviceId == null ? NO_FILTER : serviceId);
    }

    private static long number(Query query) {
        return ((Number) query.getSingleResult()).longValue();
    }

    private static List<MetricCount> counts(Query query) {
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new MetricCount((String) values[0], ((Number) values[1]).longValue());
                })
                .toList();
    }
}
