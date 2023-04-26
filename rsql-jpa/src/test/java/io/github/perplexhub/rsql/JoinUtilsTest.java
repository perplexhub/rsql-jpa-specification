package io.github.perplexhub.rsql;

import org.hibernate.query.criteria.internal.path.SingularAttributeJoin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JoinUtilsTest {

    private static final String ATTRIBUTE = "attribute";

    @Mock
    private From<?, ?> root;

    @Test
    public void testGetOrCreateJoinCreatesJoinWhenNeitherFetchNorJoinExists() {
        final Join<Object, Object> join = mock(Join.class);
        when(root.join(anyString())).thenReturn(join);

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(join, result);

        verify(root).join(ATTRIBUTE);
    }

    @Test
    public void testGetOrCreateJoinCreatesJoinWithTypeWhenNeitherFetchNorJoinExists() {
        final Join<Object, Object> join = mock(Join.class);
        when(root.join(anyString(), any(JoinType.class))).thenReturn(join);

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.LEFT);

        assertEquals(join, result);

        verify(root).join(ATTRIBUTE, JoinType.LEFT);
    }

    @Test
    public void testGetOrCreateJoinReturnsJoinWhenFetchExists() {
        final Attribute<?, ?> attribute = mock(SingularAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Fetch<?, ?> fetch = mock(SingularAttributeJoin.class);
        doReturn(attribute).when(fetch).getAttribute();

        doReturn(Collections.singleton(fetch)).when(root).getFetches();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(fetch, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    public void testGetOrCreateJoinReturnsJoinOfTypeWhenFetchExists() {
        final Attribute<?, ?> attribute = mock(SingularAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Fetch<?, ?> fetch = mock(SingularAttributeJoin.class);
        doReturn(attribute).when(fetch).getAttribute();
        when(fetch.getJoinType()).thenReturn(JoinType.RIGHT);

        doReturn(Collections.singleton(fetch)).when(root).getFetches();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.RIGHT);

        assertEquals(fetch, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    public void testGetOrCreateJoinReturnsJoinWhenJoinExists() {
        final Attribute<?, ?> attribute = mock(SingularAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Join<?, ?> join = mock(SingularAttributeJoin.class);
        doReturn(attribute).when(join).getAttribute();

        doReturn(Collections.singleton(join)).when(root).getJoins();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, null);

        assertEquals(join, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    public void testGetOrCreateJoinReturnsJoinOfTypeWhenJoinExists() {
        final Attribute<?, ?> attribute = mock(SingularAttribute.class);
        when(attribute.getName()).thenReturn(ATTRIBUTE);

        final Join<?, ?> join = mock(SingularAttributeJoin.class);
        doReturn(attribute).when(join).getAttribute();
        when(join.getJoinType()).thenReturn(JoinType.LEFT);

        doReturn(Collections.singleton(join)).when(root).getJoins();

        final Join<?, ?> result = JoinUtils.getOrCreateJoin(root, ATTRIBUTE, JoinType.LEFT);

        assertEquals(join, result);

        verify(root, never()).join(anyString());
        verify(root, never()).join(anyString(), any(JoinType.class));
    }
}
