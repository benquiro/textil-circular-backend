package cl.textilcircular.api.feature.entrepreneur;

import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.repository.*;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.entrepreneur.EntrepreneurDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntrepreneurServiceTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final EntrepreneurRepository entrepreneurs = mock(EntrepreneurRepository.class);
    private final BusinessRepository businesses = mock(BusinessRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CommuneRepository communes = mock(CommuneRepository.class);
    private final TextileServiceRepository services = mock(TextileServiceRepository.class);
    private final UserAccount user = mock(UserAccount.class);
    private EntrepreneurService service;
    private final Caller caller = new Caller(42, 1);

    @BeforeEach void setUp() {
        service = new EntrepreneurService(accounts, entrepreneurs, businesses, products, communes, services, Clock.systemUTC());
        when(accounts.findById(42L)).thenReturn(Optional.of(user));
    }
    @Test void nonEntrepreneurCannotAccessBusiness() {
        when(user.isEntrepreneur()).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.business(caller));
        verifyNoInteractions(businesses);
    }
    @Test void missingBusinessReturns404() {
        when(user.isEntrepreneur()).thenReturn(true);
        when(user.getId()).thenReturn(42L);
        when(businesses.findByOwnerId(42L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.business(caller));
    }
    @Test void productOwnedByAnotherAccountIsHidden() {
        when(user.isEntrepreneur()).thenReturn(true);
        Product p = mock(Product.class);
        Business b = mock(Business.class);
        when(products.findById(10L)).thenReturn(Optional.of(p));
        when(p.getBusiness()).thenReturn(b);
        when(b.isOwnedBy(user)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.product(caller, 10));
    }
    @Test void nonReviewerGetsEmptyQueue() {
        when(user.isAdministrator()).thenReturn(false);
        when(user.isEntrepreneur()).thenReturn(false);
        assertTrue(service.pending(caller).isEmpty());
        verifyNoInteractions(businesses);
    }
    @Test void nonReviewerCannotVote() {
        when(user.isAdministrator()).thenReturn(false);
        when(user.isEntrepreneur()).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.review(caller, 10, new ReviewRequest(null,null,null)));
        verifyNoInteractions(businesses);
    }
}
