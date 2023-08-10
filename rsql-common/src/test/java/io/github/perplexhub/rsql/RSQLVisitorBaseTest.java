package io.github.perplexhub.rsql;

import io.github.perplexhub.rsql.model.User;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.ConfigurableConversionService;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RSQLVisitorBaseTest {

    @Spy
    RSQLVisitorBase unit;
    @Mock
    ConfigurableConversionService defaultConversionService;

    @BeforeEach
    void init() {
        RSQLVisitorBase.setDefaultConversionService(defaultConversionService);
    }

    @Test
    void testConversionException() {
        assertThatExceptionOfType(ConversionException.class)
                .isThrownBy(() -> unit.convert("abc", Integer.class))
                .satisfies(e -> assertEquals("Failed to convert abc to java.lang.Integer type", e.getMessage()));

    }
}