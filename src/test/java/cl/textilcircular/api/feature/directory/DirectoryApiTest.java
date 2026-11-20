package cl.textilcircular.api.feature.directory;

import cl.textilcircular.api.core.model.analytics.AnalyticsEventType;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductAttributes;
import cl.textilcircular.api.core.repository.AnalyticsEventRepository;
import cl.textilcircular.api.support.ApiTest;
import cl.textilcircular.api.support.Fixtures;
import cl.textilcircular.api.support.PersistedData;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class DirectoryApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate tx;
    @Autowired
    AnalyticsEventRepository events;
    @Autowired
    JdbcTemplate jdbc;

    /** Directory searches have no actor, so the truncate of {@code user_account} does not reach them. */
    @AfterEach
    void clearEvents() {
        jdbc.execute("TRUNCATE analytics_event");
    }

    /** Runs {@code work} in its own committed transaction, as if another request had stored the data. */
    <T> T given(Function<PersistedData, T> work) {
        return tx.execute(status -> work.apply(new PersistedData(em)));
    }

    ResultActions call(HttpMethod method, String path, String token) throws Exception {
        var builder = request(method, "/api/v1" + path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(builder);
    }

    String login(Business business) throws Exception {
        String email = business.getOwner().getAccount().getEmail();
        String body = mvc.perform(request(HttpMethod.POST, "/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Demo1234\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    static Product product(Business business, String title) {
        return Product.create(business, business.getOwner().getAccount(), title, "Bolso hecho con retazos de mezclilla",
                15000L, List.of("https://img.example.cl/1.jpg", "/media/2.jpg"), ProductAttributes.NONE, Fixtures.NOW);
    }

    long count(AnalyticsEventType type) {
        return events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(type, Instant.EPOCH,
                Instant.now().plusSeconds(60));
    }

    @Test
    void search_shows_only_public_businesses_with_filters() throws Exception {
        given(data -> {
            data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 1L);
            data.approvedBusiness("Telar del Sur", "Tejidos", 45L, 2L);
            data.pendingBusiness("En revisión", "Desc", 102L, 1L);
            return null;
        });

        call(HttpMethod.GET, "/businesses?seed=7", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Hilos de Barrio", "Telar del Sur")))
                .andExpect(jsonPath("$.content[0].regionName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].rut").doesNotExist());
        call(HttpMethod.GET, "/businesses?seed=7&communeId=102", null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Hilos de Barrio"));
        call(HttpMethod.GET, "/businesses?seed=7&serviceId=2", null)
                .andExpect(jsonPath("$.content[0].name").value("Telar del Sur"));
        assertThat(count(AnalyticsEventType.DIRECTORY_SEARCH)).isEqualTo(3);
    }

    @Test
    void the_same_seed_keeps_pages_stable() throws Exception {
        given(data -> {
            for (int i = 0; i < 5; i++) {
                data.approvedBusiness("Taller " + i, "Desc", 102L, 1L);
            }
            return null;
        });

        String first = call(HttpMethod.GET, "/businesses?seed=42&size=5", null)
                .andReturn().getResponse().getContentAsString();
        List<String> all = JsonPath.read(first, "$.content[*].name");
        call(HttpMethod.GET, "/businesses?seed=42&size=2&page=1", null)
                .andExpect(jsonPath("$.content[0].name").value(all.get(2)))
                .andExpect(jsonPath("$.content[1].name").value(all.get(3)))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void search_needs_a_valid_seed() throws Exception {
        call(HttpMethod.GET, "/businesses", null).andExpect(status().isBadRequest());
        call(HttpMethod.GET, "/businesses?seed=0", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("seed"));
    }

    @Test
    void business_page_has_services_and_active_products() throws Exception {
        long id = given(data -> {
            Business business = data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 2L, 1L);
            em.persist(product(business, "Bolso"));
            Product hidden = product(business, "Oculto");
            hidden.hide(business.getOwner().getAccount());
            em.persist(hidden);
            return business.getId();
        });

        call(HttpMethod.GET, "/businesses/" + id, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.ownedByCaller").value(false))
                .andExpect(jsonPath("$.services[*].id").value(contains(2, 1)))
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].title").value("Bolso"))
                .andExpect(jsonPath("$.products[0].photoUrls[0]").value("https://img.example.cl/1.jpg"))
                .andExpect(jsonPath("$.products[0].photoUrls[1]").value(startsWith("http")));
        assertThat(count(AnalyticsEventType.BUSINESS_VIEW)).isEqualTo(1);
    }

    @Test
    void a_business_that_is_not_public_is_only_for_its_owner() throws Exception {
        Business pending = given(data -> data.pendingBusiness("En revisión", "Desc", 102L, 1L));

        call(HttpMethod.GET, "/businesses/" + pending.getId(), null).andExpect(status().isNotFound());
        call(HttpMethod.GET, "/businesses/" + pending.getId(), login(pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.ownedByCaller").value(true));
        call(HttpMethod.GET, "/businesses/999999", null).andExpect(status().isNotFound());
    }

    @Test
    void product_page_includes_its_business_card() throws Exception {
        Product product = given(data -> {
            Product p = product(data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 1L), "Bolso");
            em.persist(p);
            return p;
        });

        call(HttpMethod.GET, "/products/" + product.getId(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.title").value("Bolso"))
                .andExpect(jsonPath("$.product.price").value(15000))
                .andExpect(jsonPath("$.business.name").value("Hilos de Barrio"))
                .andExpect(jsonPath("$.ownedByCaller").value(false));
        assertThat(count(AnalyticsEventType.PRODUCT_VIEW)).isEqualTo(1);
    }

    @Test
    void a_hidden_product_is_only_for_its_owner() throws Exception {
        Product hidden = given(data -> {
            Business business = data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 1L);
            Product p = product(business, "Oculto");
            p.hide(business.getOwner().getAccount());
            em.persist(p);
            return p;
        });

        call(HttpMethod.GET, "/products/" + hidden.getId(), null).andExpect(status().isNotFound());
        call(HttpMethod.GET, "/products/" + hidden.getId(), login(hidden.getBusiness()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.status").value("HIDDEN"))
                .andExpect(jsonPath("$.ownedByCaller").value(true));
    }

    @Test
    void contact_clicks_are_recorded_for_public_businesses() throws Exception {
        long publicId = given(data -> data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 1L).getId());
        long pendingId = given(data -> data.pendingBusiness("En revisión", "Desc", 102L, 1L).getId());

        call(HttpMethod.POST, "/businesses/" + publicId + "/contact-clicks", null).andExpect(status().isNoContent());
        mvc.perform(request(HttpMethod.POST, "/api/v1/businesses/" + publicId + "/contact-clicks")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1}"))
                .andExpect(status().isNoContent());
        call(HttpMethod.POST, "/businesses/" + pendingId + "/contact-clicks", null).andExpect(status().isNotFound());
        assertThat(count(AnalyticsEventType.CONTACT_CLICK)).isEqualTo(2);
    }

    @Test
    void hidden_businesses_leave_the_search() throws Exception {
        long id = given(data -> {
            Business business = data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 1L);
            business.hide(Fixtures.NOW);
            return business.getId();
        });

        call(HttpMethod.GET, "/businesses?seed=1", null)
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) id))));
        call(HttpMethod.GET, "/businesses/" + id, null).andExpect(status().isNotFound());
    }
}
