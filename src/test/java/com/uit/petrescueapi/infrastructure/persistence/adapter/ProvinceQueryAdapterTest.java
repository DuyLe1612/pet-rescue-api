package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProvinceQueryAdapterTest {

    final ObjectMapper mapper = spy(new ObjectMapper());
    final ProvinceQueryAdapter adapter = new ProvinceQueryAdapter(mapper);

    @Test
    void theAddressFileIsParsedOnlyOnce() throws Exception {
        adapter.listProvinces(null, 0, 10);
        adapter.listProvinces("a", 0, 10);
        adapter.getProvinceDetail(firstCode(), 2);

        verify(mapper, times(1)).readValue(any(InputStream.class), any(TypeReference.class));
    }

    @Test
    void askingWithoutWardsDoesNotRemoveThemForLaterRequests() {
        int code = firstCode();

        assertThat(adapter.getProvinceDetail(code, 1).getWards()).isEmpty();

        assertThat(adapter.getProvinceDetail(code, 2).getWards()).isNotEmpty();
    }

    private int firstCode() {
        return adapter.listProvinces(null, 0, 1).getContent().get(0).getCode();
    }
}
