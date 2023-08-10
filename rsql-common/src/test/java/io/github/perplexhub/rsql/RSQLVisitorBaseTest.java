package io.github.perplexhub.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.NonNull;

@ExtendWith(MockitoExtension.class)
class RSQLVisitorBaseTest {

  @Spy
  RSQLVisitorBase<?, ?> unit;

  @BeforeEach
  void init() {
    RSQLVisitorBase.setDefaultConversionService(new DefaultConversionService());
  }

  @Test
  void testConversionException() {
    assertThatExceptionOfType(ConversionException.class)
        .isThrownBy(() -> unit.convert("abc", Integer.class))
        .satisfies(e -> assertEquals("Failed to convert abc to java.lang.Integer type", e.getMessage()));
  }

  @Nested
  class ConvertLocalDateToLocalDateTime {

    @AfterEach
    void tearDown() {
      RSQLVisitorBase.defaultConversionService.removeConvertible(String.class, LocalDate.class);
      RSQLVisitorBase.defaultConversionService.removeConvertible(String.class, LocalDateTime.class);
    }

    @Test
    void shouldConvertToLocalDateTimeIfCanConvertToLocalDate() {
      //given
      var source = "2023-08-10";
      var targetType = LocalDateTime.class;

      //when
      var actual = unit.convert(source, targetType);

      //then
      assertThat(actual)
          .isInstanceOf(LocalDateTime.class)
          .asInstanceOf(LOCAL_DATE_TIME)
          .isEqualTo("%sT00:00:00".formatted(source));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-19-10", "", "abc"})
    void shouldThrowIfCannotConvertToLocalDateTime(String source) {
      assertThatExceptionOfType(ConversionException.class)
          .isThrownBy(() -> unit.convert(source, LocalDateTime.class));
    }

    @Test
    @DisplayName("Should convert with Spring converters if available")
    void shouldConvertToLocalDateTimeWhenCanConvertWithSpringConverters() {
      //given
      var source = "2023-08-10";

      var spyStringToLocalDateConverter = spy(new StringToLocalDateConverter());
      var spyStringToLocalDateTimeConverter = spy(new StringToLocalDateTimeConverter());
      RSQLVisitorBase.defaultConversionService.addConverter(spyStringToLocalDateConverter);
      RSQLVisitorBase.defaultConversionService.addConverter(spyStringToLocalDateTimeConverter);

      var targetType = LocalDateTime.class;

      //when
      var actual = unit.convert(source, targetType);

      //then
      assertThat(actual)
          .isInstanceOf(LocalDateTime.class)
          .asInstanceOf(LOCAL_DATE_TIME)
          .isEqualTo("%sT00:00:00".formatted(source));

      verify(spyStringToLocalDateTimeConverter).convert(source);
      verify(spyStringToLocalDateConverter).convert(source);
    }

    @Test
    @DisplayName("Should convert when String -> LocalDateTime fails and there is no String -> LocalDate")
    void shouldConvertToLocalDateTimeWhenNoSpringConverter() {
      //given
      var source = "2023-08-10";
      RSQLVisitorBase.defaultConversionService.addConverter(new StringToLocalDateTimeConverter());
      var targetType = LocalDateTime.class;

      //when
      var actual = unit.convert(source, targetType);

      //then
      assertThat(actual)
          .isInstanceOf(LocalDateTime.class)
          .asInstanceOf(LOCAL_DATE_TIME)
          .isEqualTo("%sT00:00:00".formatted(source));
    }
  }

  /**
   * Non-final class to spy on it.
   */
  private static class StringToLocalDateConverter implements Converter<String, LocalDate> {

    @NonNull
    @Override
    public LocalDate convert(@NonNull String source) {
      return LocalDate.parse(source, DateTimeFormatter.ISO_DATE);
    }
  }

  /**
   * Non-final class to spy on it.
   */
  private static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    @NonNull
    @Override
    public LocalDateTime convert(@NonNull String source) {
      return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
    }
  }
}