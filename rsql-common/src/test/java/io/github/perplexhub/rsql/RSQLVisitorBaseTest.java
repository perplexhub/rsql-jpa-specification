package io.github.perplexhub.rsql;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Collections;
import java.util.Set;

import static com.github.valfirst.slf4jtest.LoggingEvent.debug;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

@ExtendWith({MockitoExtension.class, TestLoggerFactoryExtension.class})
class RSQLVisitorBaseTest {

    TestLogger log = TestLoggerFactory.getTestLogger(RSQLVisitorBase.class);
    @Mock
    RSQLVisitorBase instance;

    @BeforeEach
    void setUp() {
        RSQLVisitorBase.setDefaultConversionService(new DefaultConversionService());
        doCallRealMethod().when(instance).convert(any(), any());
        TestLoggerFactory.clear();
    }

    @Test
    void test_conversion_success() {
        RSQLVisitorBase.setGlobalSuppressConversionExceptions(Collections.emptySet());
        instance.convert("123", Integer.class);
        assertThat(log.getLoggingEvents(), is(asList(debug("convert(source:{},targetType:{})", new Object[]{"123", Integer.class}))));
    }

    @Test
    void test_conversion_exception() {
        RSQLVisitorBase.setGlobalSuppressConversionExceptions(Collections.emptySet());
        instance.convert("abc", Integer.class);
        assertThat(log.getLoggingEvents().get(0), is(debug("convert(source:{},targetType:{})", new Object[]{"abc", Integer.class})));
        var exception = log.getLoggingEvents().get(1);
        assertThat(exception.getThrowable().get().getClass(), is(ConversionFailedException.class));
        assertThat(exception.getLevel(), is(Level.ERROR));
        assertThat(exception.getMessage(), is("Parsing [{}] with [{}] causing [{}], add your parser via RSQLSupport.addConverter(Type.class, Type::valueOf)"));
        assertThat(exception.getArguments().get(0), is("abc"));
        assertThat(exception.getArguments().get(1), is("java.lang.Integer"));
        assertThat(exception.getArguments().get(2).toString(), is("Failed to convert from type [java.lang.String] to type [java.lang.Integer] for value 'abc'"));
    }

    @Test
    void test_conversion_exception_suppression() {
        RSQLVisitorBase.setGlobalSuppressConversionExceptions(Set.of(ConversionFailedException.class));
        instance.convert("abc", Integer.class);
        assertThat(log.getLoggingEvents().get(0), is(debug("convert(source:{},targetType:{})", new Object[]{"abc", Integer.class})));
        assertThat(log.getLoggingEvents().get(1), is(debug("Parsing [{}] with [{}] causing [{}], skip", new Object[]{"abc", "java.lang.Integer", "Failed to convert from type [java.lang.String] to type [java.lang.Integer] for value 'abc'"})));
    }
}