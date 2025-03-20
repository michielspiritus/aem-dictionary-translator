package be.orbinson.aem.dictionarytranslator.services.impl;

import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Version;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CombiningMessageEntryResourceProviderTest {

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    CombiningMessageEntryResourceProvider resourceProvider;

    @Mock
    ProductInfoProvider productInfoProvider;

    @Mock
    ProductInfo productInfo;

    @Mock
    Version version;

    @BeforeEach
    void setup() {
        context.registerService(Replicator.class, mock(Replicator.class));
        productInfoProvider = context.registerService(ProductInfoProvider.class, productInfoProvider);
        context.registerInjectActivateService(new DictionaryServiceImpl());

        resourceProvider = context.registerInjectActivateService(new CombiningMessageEntryResourceProvider());

        context.load().json("/content.json", "/content");
    }

    @Test
    void rootPathShouldNotReturnNull() {
        assertNotNull(context.resourceResolver().getResource("/mnt/dictionary"));
    }

    @Test
    void syntheticPathShouldReturnNonNullResource() {
        when(productInfoProvider.getProductInfo()).thenReturn(productInfo);
        when(productInfo.getVersion()).thenReturn(version);

        assertNotNull(context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple"));
    }

    @Test
    void listChildrenShouldNotFailOnResource() {
        when(productInfoProvider.getProductInfo()).thenReturn(productInfo);
        when(productInfo.getVersion()).thenReturn(version);

        assertDoesNotThrow(() -> context.resourceResolver().getResource("/mnt/dictionary/content/dictionaries/fruit/i18n/apple").listChildren());
    }
}
