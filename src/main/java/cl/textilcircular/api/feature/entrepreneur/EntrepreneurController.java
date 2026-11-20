package cl.textilcircular.api.feature.entrepreneur;

import cl.textilcircular.api.core.model.product.ProductStatus;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.entrepreneur.EntrepreneurDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Routes are relative to /api/v1, configured centrally in WebConfig. */
@RestController
@RequiredArgsConstructor
class EntrepreneurController {
    private final EntrepreneurService service;
    @GetMapping("/me/business") MyBusiness business(@AuthenticationPrincipal Jwt jwt) { return service.business(Caller.of(jwt)); }
    @PutMapping("/me/business") MyBusiness editBusiness(@AuthenticationPrincipal Jwt jwt, @RequestBody BusinessDraft draft) { return service.editBusiness(Caller.of(jwt), draft); }
    @PutMapping("/me/business/visibility") MyBusiness visibility(@AuthenticationPrincipal Jwt jwt, @RequestBody VisibilityRequest request) { return service.businessVisibility(Caller.of(jwt), request); }
    @GetMapping("/me/products") List<ProductView> products(@AuthenticationPrincipal Jwt jwt, @RequestParam(required=false) ProductStatus status) { return service.products(Caller.of(jwt), status); }
    @PostMapping("/me/products") @ResponseStatus(HttpStatus.CREATED) ProductView create(@AuthenticationPrincipal Jwt jwt, @RequestBody ProductRequest request) { return service.create(Caller.of(jwt), request); }
    @GetMapping("/me/products/{productId}") ProductView product(@AuthenticationPrincipal Jwt jwt, @PathVariable long productId) { return service.product(Caller.of(jwt), productId); }
    @PutMapping("/me/products/{productId}") ProductView update(@AuthenticationPrincipal Jwt jwt, @PathVariable long productId, @RequestBody ProductRequest request) { return service.update(Caller.of(jwt), productId, request); }
    @PutMapping("/me/products/{productId}/visibility") ProductView productVisibility(@AuthenticationPrincipal Jwt jwt, @PathVariable long productId, @RequestBody VisibilityRequest request) { return service.productVisibility(Caller.of(jwt), productId, request); }
    @DeleteMapping("/me/products/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long productId) { service.delete(Caller.of(jwt), productId); }
    @GetMapping("/reviews/pending") List<PendingReview> pending(@AuthenticationPrincipal Jwt jwt) { return service.pending(Caller.of(jwt)); }
    @PostMapping("/businesses/{businessId}/reviews") ReviewResult review(@AuthenticationPrincipal Jwt jwt, @PathVariable long businessId, @RequestBody ReviewRequest request) { return service.review(Caller.of(jwt), businessId, request); }
}
