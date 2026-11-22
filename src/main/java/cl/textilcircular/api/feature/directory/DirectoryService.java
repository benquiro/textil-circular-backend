package cl.textilcircular.api.feature.directory;

import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.analytics.AnalyticsEvent;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductStatus;
import cl.textilcircular.api.core.repository.AnalyticsEventRepository;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.repository.CatalogEntryRepository;
import cl.textilcircular.api.core.repository.ProductRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.directory.DirectoryDtos.BusinessCard;
import cl.textilcircular.api.feature.directory.DirectoryDtos.BusinessCardPage;
import cl.textilcircular.api.feature.directory.DirectoryDtos.BusinessDetail;
import cl.textilcircular.api.feature.directory.DirectoryDtos.ProductDetail;
import cl.textilcircular.api.feature.directory.DirectoryDtos.ProductItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The public directory. Guests and signed-in users read the same data; a session only adds that the owner
 * also sees their own business and products while they are not public. Every read leaves an analytics
 * event for the admin metrics.
 */
@Service
@RequiredArgsConstructor
@Transactional
class DirectoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CatalogEntryRepository catalog;
    private final BusinessRepository businesses;
    private final ProductRepository products;
    private final UserAccountRepository accounts;
    private final AnalyticsEventRepository events;
    private final Clock clock;

    /** Filters are optional; the same {@code seed} keeps the shuffled order stable across pages. */
    BusinessCardPage search(String q, Long regionId, Long communeId, Long serviceId, int seed, int page, int size) {
        if (seed < 1) {
            throw new ValidationException("seed", "La semilla debe ser un número mayor que 0");
        }
        Page<CatalogEntry> found = catalog.search(q, regionId, communeId, serviceId, String.valueOf(seed),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        events.save(AnalyticsEvent.directorySearch(q, regionId, communeId, serviceId, clock.instant()));
        return new BusinessCardPage(found.map(BusinessCard::of).getContent(), found.getNumber(), found.getSize(),
                found.getTotalElements(), found.isLast());
    }

    /** Public businesses for everyone; a pending, rejected or hidden one only for its owner. */
    BusinessDetail business(Caller caller, long businessId) {
        UserAccount viewer = viewer(caller);
        Business business = businesses.findById(businessId).orElseThrow(NotFoundException::new);
        boolean owned = viewer != null && business.isOwnedBy(viewer);
        if (business.isDeleted() || !(business.isPublic() || owned)) {
            throw new NotFoundException();
        }
        events.save(AnalyticsEvent.businessView(viewer, business, clock.instant()));
        return BusinessDetail.of(business, owned, products.findOwnerCatalog(businessId, ProductStatus.ACTIVE));
    }

    /** Public when the product is active and its business is public; the owner always sees it. */
    ProductDetail product(Caller caller, long productId) {
        UserAccount viewer = viewer(caller);
        Product product = products.findById(productId).orElseThrow(NotFoundException::new);
        Business business = product.getBusiness();
        boolean owned = viewer != null && business.isOwnedBy(viewer);
        if (product.isDeleted() || business.isDeleted() || !(product.isPublic() || owned)) {
            throw new NotFoundException();
        }
        events.save(AnalyticsEvent.productView(viewer, product, clock.instant()));
        return new ProductDetail(ProductItem.of(product), BusinessCard.of(business), owned);
    }

    /**
     * Only public businesses can be contacted. {@code productId} is accepted but not stored: the
     * {@code analytics_event} table keeps contact clicks per business (see its check constraint).
     */
    void recordContactClick(Caller caller, long businessId) {
        Business business = businesses.findById(businessId)
                .filter(Business::isPublic)
                .orElseThrow(NotFoundException::new);
        events.save(AnalyticsEvent.contactClick(viewer(caller), business, clock.instant()));
    }

    /** {@code null} for guests. A token of an account that no longer exists is read as a guest. */
    private UserAccount viewer(Caller caller) {
        if (caller == null) {
            return null;
        }
        return accounts.findById(caller.userId())
                .filter(a -> a.getStatus() != AccountStatus.DELETED)
                .orElse(null);
    }
}
