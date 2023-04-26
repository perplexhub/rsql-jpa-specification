package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.Attribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinUtilsTest {

    private static final String ATTRIBUTE = "attribute";

    @Mock
    private From<?, ?> root;

    @Test
    void testGetOrCreateJoinCreatesJoinWhenNeitherFetchNorJoinExists() {
        final Join<Object, Object> join = mock(Join.class);
        when(root.join(anyString())).thenReturn(join);

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(join, result);

        verify(root).join(ATTRIBUTE);
    }

    @Test
    void testGetOrCreateJoinCreatesJoinWithTypeWhenNeitherFetchNorJoinExists() {
        final Join<Object, Object> join = mock(Join.class);
        when(root.join(anyString(), any(JoinType.class))).thenReturn(join);

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.LEFT);

        assertEquals(join, result);

        verify(root).join(ATTRIBUTE, JoinType.LEFT);
    }

    @Test
    void testGetOrCreateJoinReturnsJoinWhenFetchExists() {
        final Attribute<?, ?> attribute = mock(SingularPersistentAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Fetch<?, ?> fetch = mock(SqmSingularJoin.class);
        doReturn(attribute).when(fetch).getAttribute();

        doReturn(Collections.singleton(fetch)).when(root).getFetches();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(fetch, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    void testGetOrCreateJoinReturnsJoinOfTypeWhenFetchExists() {
        final Attribute<?, ?> attribute = mock(SingularPersistentAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Fetch<?, ?> fetch = mock(SqmSingularJoin.class);
        doReturn(attribute).when(fetch).getAttribute();
        when(fetch.getJoinType()).thenReturn(JoinType.RIGHT);

        doReturn(Collections.singleton(fetch)).when(root).getFetches();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.RIGHT);

        assertEquals(fetch, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    void testGetOrCreateJoinReturnsJoinWhenJoinExists() {
        final Attribute<?, ?> attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Join<?, ?> join = mock(Join.class);
        doReturn(attribute).when(join).getAttribute();

        doReturn(Collections.singleton(join)).when(root).getJoins();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(join, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    void testGetOrCreateJoinReturnsJoinOfTypeWhenJoinExists() {
        final Attribute<?, ?> attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Join<?, ?> join = mock(Join.class);
        doReturn(attribute).when(join).getAttribute();
        when(join.getJoinType()).thenReturn(JoinType.LEFT);

        doReturn(Collections.singleton(join)).when(root).getJoins();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.LEFT);

        assertEquals(join, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }
}
