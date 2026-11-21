package cl.textilcircular.api.feature.entrepreneur;

import cl.textilcircular.api.core.error.*;
import cl.textilcircular.api.core.model.account.*;
import cl.textilcircular.api.core.model.business.*;
import cl.textilcircular.api.core.model.product.*;
import cl.textilcircular.api.core.model.reference.*;
import cl.textilcircular.api.core.repository.*;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.core.validation.Validators;
import cl.textilcircular.api.feature.entrepreneur.EntrepreneurDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
class EntrepreneurService {
    private final UserAccountRepository accounts;
    private final EntrepreneurRepository entrepreneurs;
    private final BusinessRepository businesses;
    private final ProductRepository products;
    private final CommuneRepository communes;
    private final TextileServiceRepository services;
    private final Clock clock;

    private UserAccount active(Caller caller) {
        UserAccount account = accounts.findById(caller.userId()).orElseThrow(UnauthorizedException::new);
        account.ensureActive();
        return account;
    }
    private UserAccount owner(Caller caller) {
        UserAccount account = active(caller);
        if (!account.isEntrepreneur()) throw new ForbiddenException();
        return account;
    }
    private Business ownedBusiness(UserAccount account) {
        return businesses.findByOwnerId(account.getId()).filter(b->!b.isDeleted()).orElseThrow(NotFoundException::new);
    }
    private Product ownedProduct(UserAccount account, long id) {
        Product p = products.findById(id).filter(x->!x.isDeleted()).orElseThrow(NotFoundException::new);
        if (!p.getBusiness().isOwnedBy(account)) throw new NotFoundException();
        return p;
    }
    private boolean reviewerEligible(UserAccount account) {
        return account.isAdministrator() || (account.isEntrepreneur() &&
                businesses.existsByOwnerIdAndStatusAndDeletedAtIsNull(account.getId(), BusinessStatus.APPROVED));
    }
    private long pendingCount(UserAccount account) {
        return reviewerEligible(account) ? businesses.findReviewQueue(account.getId(), PageRequest.of(0, 1)).getTotalElements() : 0;
    }
    private MyBusiness businessView(Business b, UserAccount account) {
        long id=b.getId();
        return MyBusiness.of(b, new Counts(products.countByBusinessIdAndDeletedAtIsNull(id),
                products.countByBusinessIdAndStatusAndDeletedAtIsNull(id,ProductStatus.ACTIVE),
                products.countByBusinessIdAndStatusAndDeletedAtIsNull(id,ProductStatus.HIDDEN), pendingCount(account)));
    }
    @Transactional(readOnly=true) MyBusiness business(Caller caller) {
        UserAccount a=owner(caller); return businessView(ownedBusiness(a), a);
    }
    MyBusiness editBusiness(Caller caller, BusinessDraft draft) {
        UserAccount a=owner(caller); Business b=ownedBusiness(a);
        if (draft == null) throw new ValidationException("business", "Completa los datos del emprendimiento");
        Commune commune=draft.communeId()==null?null:communes.findById(draft.communeId()).orElse(null);
        List<TextileService> chosen = draft.serviceIds()==null ? null : draft.serviceIds().stream()
                .map(id -> id==null?null:services.findById(id).orElse(null))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        b.edit(draft.name(),draft.description(),draft.photoUrl(),commune,chosen,clock.instant());
        return businessView(b,a);
    }
    MyBusiness businessVisibility(Caller caller, VisibilityRequest request) {
        UserAccount a=owner(caller); Business b=ownedBusiness(a);
        if(request==null) throw new ValidationException("hidden","Indica la visibilidad");
        if(request.hidden()) b.hide(clock.instant()); else b.show();
        return businessView(b,a);
    }
    @Transactional(readOnly=true) List<ProductView> products(Caller caller, ProductStatus status) {
        return products.findOwnerCatalog(ownedBusiness(owner(caller)).getId(),status).stream().map(ProductView::of).toList();
    }
    @Transactional(readOnly=true) ProductView product(Caller caller,long id) { return ProductView.of(ownedProduct(owner(caller),id)); }
    private TextileService selectedService(Business b, Long id) {
        if(id==null) return null;
        return b.getServices().stream().filter(s->s.getId().equals(id)).findFirst()
                .orElseThrow(()->new ValidationException("serviceId","El servicio debe pertenecer a tu emprendimiento"));
    }
    private ProductAttributes attributes(ProductRequest r) { return new ProductAttributes(r.size(),r.condition(),r.material(),r.offeringType()); }
    ProductView create(Caller caller,ProductRequest r) {
        UserAccount a=owner(caller); Business b=ownedBusiness(a);
        if(r==null) throw new ValidationException("product","Completa los datos del producto");
        TextileService s=selectedService(b,r.serviceId());
        Product p=Product.create(b,a,r.title(),r.description(),r.price(),r.photoUrls(),attributes(r),clock.instant());
        p.assignService(s);
        return ProductView.of(products.saveAndFlush(p));
    }
    ProductView update(Caller caller,long id,ProductRequest r) {
        UserAccount a=owner(caller); Product p=ownedProduct(a,id);
        if(r==null) throw new ValidationException("product","Completa los datos del producto");
        TextileService s=selectedService(p.getBusiness(),r.serviceId());
        p.update(a,r.title(),r.description(),r.price(),r.photoUrls(),attributes(r),clock.instant());
        p.assignService(s);
        return ProductView.of(p);
    }
    ProductView productVisibility(Caller caller,long id,VisibilityRequest r) {
        UserAccount a=owner(caller); Product p=ownedProduct(a,id);
        if(r==null) throw new ValidationException("hidden","Indica la visibilidad");
        if(r.hidden()) p.hide(a); else p.show(a);
        return ProductView.of(p);
    }
    void delete(Caller caller,long id) { UserAccount a=owner(caller); ownedProduct(a,id).delete(a,clock.instant()); }
    @Transactional(readOnly=true) List<PendingReview> pending(Caller caller) {
        UserAccount a=active(caller);
        if(!reviewerEligible(a)) return List.of();
        return businesses.findReviewQueue(a.getId(),PageRequest.of(0,10000)).stream().map(PendingReview::of).toList();
    }
    ReviewResult review(Caller caller,long id,ReviewRequest r) {
        UserAccount a=active(caller);
        if(!reviewerEligible(a)) throw new ForbiddenException();
        if(r==null) throw new ValidationException("decision","Elige una decisión");
        Business b=businesses.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        b.vote(a,r.decision(),r.rejectionReason(),r.comment(),clock.instant());
        return new ReviewResult(b.getStatus());
    }
}
