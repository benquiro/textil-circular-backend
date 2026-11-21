package cl.textilcircular.api.feature.entrepreneur;

import cl.textilcircular.api.core.model.business.*;
import cl.textilcircular.api.core.model.product.*;
import cl.textilcircular.api.core.model.reference.TextileService;
import java.time.Instant;
import java.util.List;

/** JSON contracts in docs/api/openapi.yaml; never expose JPA entities directly. */
public final class EntrepreneurDtos {
    private EntrepreneurDtos() {}
    public record BusinessDraft(String name, String description, String photoUrl, Long communeId, List<Long> serviceIds) {}
    public record VisibilityRequest(boolean hidden) {}
    public record ServiceView(long id, String name) {
        static ServiceView of(TextileService s) { return new ServiceView(s.getId(), s.getName()); }
    }
    public record ReviewView(long id, int round, ReviewDecision decision, RejectionReason rejectionReason,
                             String comment, Instant reviewedAt) {
        static ReviewView of(BusinessReview r) { return new ReviewView(r.getId(), r.getReviewRound(), r.getDecision(), r.getRejectionReason(), r.getComment(), r.getReviewedAt()); }
    }
    public record Counts(long productsTotal, long productsActive, long productsHidden, long pendingReviews) {}
    public record MyBusiness(long id, String name, String description, String photoUrl, long communeId,
                             String communeName, long regionId, String regionName, BusinessStatus status,
                             int reviewRound, boolean hidden, Instant approvedAt,
                             List<ServiceView> services, List<ReviewView> reviews, Counts counts) {
        static MyBusiness of(Business b, Counts counts) {
            return new MyBusiness(b.getId(), b.getName(), b.getDescription(), b.getPhotoUrl(),
                    b.getCommune().getId(), b.getCommune().getName(), b.getCommune().getRegion().getId(),
                    b.getCommune().getRegion().getName(), b.getStatus(), b.getReviewRound(), b.getHiddenAt()!=null,
                    b.getApprovedAt(), b.getServices().stream().map(ServiceView::of).toList(),
                    b.getReviews().stream().sorted((a,c)->c.getReviewedAt().compareTo(a.getReviewedAt())).map(ReviewView::of).toList(), counts);
        }
    }
    public record ProductRequest(String title, String description, Long price, List<String> photoUrls,
                                 Long serviceId, ProductSize size, ProductCondition condition,
                                 String material, OfferingType offeringType) {}
    public record ProductView(long id, long businessId, String title, String description, Long price,
                              ProductStatus status, List<String> photoUrls, Long serviceId, ProductSize size,
                              ProductCondition condition, String material, OfferingType offeringType) {
        static ProductView of(Product p) {
            return new ProductView(p.getId(), p.getBusiness().getId(), p.getTitle(), p.getDescription(),
                    p.getPrice(), p.getStatus(), p.getPhotos().stream().map(ProductPhoto::getUrl).toList(),
                    p.getService() == null ? null : p.getService().getId(), p.getSize(), p.getCondition(),
                    p.getMaterial(), p.getOfferingType());
        }
    }
    public record PendingReview(long id, String name, String description, String photoUrl,
                                long communeId, String communeName, long regionId, String regionName,
                                int reviewRound, long approvals, int requiredApprovals, List<ServiceView> services) {
        static PendingReview of(Business b) {
            return new PendingReview(b.getId(), b.getName(), b.getDescription(), b.getPhotoUrl(),
                    b.getCommune().getId(), b.getCommune().getName(), b.getCommune().getRegion().getId(),
                    b.getCommune().getRegion().getName(), b.getReviewRound(),
                    b.currentRoundReviews().stream().filter(r->r.getDecision()==ReviewDecision.APPROVED).count(),
                    2, b.getServices().stream().map(ServiceView::of).toList());
        }
    }
    public record ReviewRequest(ReviewDecision decision, RejectionReason rejectionReason, String comment) {}
    public record ReviewResult(BusinessStatus businessStatus) {}
}
