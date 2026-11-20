package cl.textilcircular.api.feature.directory;

import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.BusinessStatus;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.model.product.OfferingType;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductCondition;
import cl.textilcircular.api.core.model.product.ProductPhoto;
import cl.textilcircular.api.core.model.product.ProductSize;
import cl.textilcircular.api.core.model.product.ProductStatus;
import cl.textilcircular.api.core.model.reference.TextileService;
import cl.textilcircular.api.core.web.MediaUrls;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Response bodies of the public directory, as named in the API contract. None carries RUT or email. */
final class DirectoryDtos {

    private DirectoryDtos() {
    }

    @Schema(name = "BusinessCard")
    record BusinessCard(long id, String name, String description, String photoUrl, long communeId,
                        String communeName, long regionId, String regionName) {

        /** A row of {@code v_catalog}: already public. */
        static BusinessCard of(CatalogEntry entry) {
            return new BusinessCard(entry.getId(), entry.getName(), entry.getDescription(),
                    MediaUrls.absolute(entry.getPhotoUrl()), entry.getCommuneId(), entry.getCommuneName(),
                    entry.getRegionId(), entry.getRegionName());
        }

        /** Built from the entity, because the owner also sees a business that is not in {@code v_catalog}. */
        static BusinessCard of(Business business) {
            var commune = business.getCommune();
            return new BusinessCard(business.getId(), business.getName(), business.getDescription(),
                    MediaUrls.absolute(business.getPhotoUrl()), commune.getId(), commune.getName(),
                    commune.getRegion().getId(), commune.getRegion().getName());
        }
    }

    @Schema(name = "BusinessCardPage")
    record BusinessCardPage(List<BusinessCard> content, @Schema(description = "Base cero") int page, int size,
                            @Schema(description = "Cantidad de resultados") long totalElements,
                            @Schema(description = "Verdadero en la última página") boolean last) {
    }

    @Schema(name = "Service")
    record ServiceItem(long id, String name) {

        static ServiceItem of(TextileService service) {
            return new ServiceItem(service.getId(), service.getName());
        }
    }

    /** {@code serviceId} is always null: products do not store a service yet (V5__product.sql). */
    @Schema(name = "Product")
    record ProductItem(long id, long businessId, String title, String description, Long price,
                       ProductStatus status,
                       @Schema(description = "En orden; la primera es la foto principal") List<String> photoUrls,
                       Long serviceId, ProductSize size, ProductCondition condition, String material,
                       OfferingType offeringType) {

        static ProductItem of(Product product) {
            List<String> photos = product.getPhotos().stream()
                    .map(ProductPhoto::getUrl).map(MediaUrls::absolute).toList();
            return new ProductItem(product.getId(), product.getBusiness().getId(), product.getTitle(),
                    product.getDescription(), product.getPrice(), product.getStatus(), photos, null,
                    product.getSize(), product.getCondition(), product.getMaterial(), product.getOfferingType());
        }
    }

    /** {@link BusinessCard} plus status, services and active products (contract: allOf). */
    @Schema(name = "BusinessDetail")
    record BusinessDetail(long id, String name, String description, String photoUrl, long communeId,
                          String communeName, long regionId, String regionName, BusinessStatus status,
                          boolean ownedByCaller, List<ServiceItem> services, List<ProductItem> products) {

        static BusinessDetail of(Business business, boolean ownedByCaller, List<Product> products) {
            BusinessCard card = BusinessCard.of(business);
            return new BusinessDetail(card.id(), card.name(), card.description(), card.photoUrl(),
                    card.communeId(), card.communeName(), card.regionId(), card.regionName(),
                    business.getStatus(), ownedByCaller,
                    business.getServices().stream().map(ServiceItem::of).toList(),
                    products.stream().map(ProductItem::of).toList());
        }
    }

    @Schema(name = "ProductDetail")
    record ProductDetail(ProductItem product, BusinessCard business, boolean ownedByCaller) {
    }

    /** Body of {@code POST /businesses/{id}/contact-clicks}; it may be empty. */
    record ContactClickRequest(@Schema(description = "Cuando el contacto se hizo desde un producto") Long productId) {
    }
}
