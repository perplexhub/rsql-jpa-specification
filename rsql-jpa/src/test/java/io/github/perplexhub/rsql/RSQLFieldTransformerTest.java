package io.github.perplexhub.rsql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import io.github.perplexhub.rsql.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;

@ExtendWith(MockitoExtension.class)
class RSQLFieldTransformerTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<User> criteriaQuery;

    @Mock
    private Root<User> root;

    @Mock
    private TypedQuery<User> typedQuery;

    private Map<String, String> propertyPathMapper;

    @BeforeEach
    void setUp() {
        propertyPathMapper = Collections.emptyMap();
        
        // Setup field transformers
        RSQLCommonSupport.addFieldTransformer(User.class, "number", 
            value -> value.replaceAll("[^0-9]", ""));
        
        RSQLCommonSupport.addFieldTransformer(User.class, "email", 
            value -> value.toLowerCase());
        
        RSQLCommonSupport.addFieldTransformer(User.class, "phone", 
            value -> value.replaceAll("[^0-9+]", ""));
    }

    @Test
    void testNumberFieldTransformer() {
        // Given
        String rsqlQuery = "number=like='123????'";
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(root.get("number")).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%123%"))).thenReturn(mock());

        // When
        Node rootNode = new RSQLParser().parse(rsqlQuery);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(criteriaBuilder, propertyPathMapper);
        Predicate predicate = rootNode.accept(converter, root);

        // Then
        assertNotNull(predicate);
        verify(criteriaBuilder).like(any(), eq("%123%"));
    }

    @Test
    void testEmailFieldTransformer() {
        // Given
        String rsqlQuery = "email=like='TEST@EXAMPLE.COM'";
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(root.get("email")).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%test@example.com%"))).thenReturn(mock());

        // When
        Node rootNode = new RSQLParser().parse(rsqlQuery);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(criteriaBuilder, propertyPathMapper);
        Predicate predicate = rootNode.accept(converter, root);

        // Then
        assertNotNull(predicate);
        verify(criteriaBuilder).like(any(), eq("%test@example.com%"));
    }

    @Test
    void testPhoneFieldTransformer() {
        // Given
        String rsqlQuery = "phone=like='+1 (555) 123-4567'";
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(root.get("phone")).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%+15551234567%"))).thenReturn(mock());

        // When
        Node rootNode = new RSQLParser().parse(rsqlQuery);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(criteriaBuilder, propertyPathMapper);
        Predicate predicate = rootNode.accept(converter, root);

        // Then
        assertNotNull(predicate);
        verify(criteriaBuilder).like(any(), eq("%+15551234567%"));
    }

    @Test
    void testMultipleTransformers() {
        // Given
        String rsqlQuery = "number=like='123????' and email=like='TEST@EXAMPLE.COM'";
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(root.get("number")).thenReturn(mock());
        when(root.get("email")).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%123%"))).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%test@example.com%"))).thenReturn(mock());
        when(criteriaBuilder.and(any(), any())).thenReturn(mock());

        // When
        Node rootNode = new RSQLParser().parse(rsqlQuery);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(criteriaBuilder, propertyPathMapper);
        Predicate predicate = rootNode.accept(converter, root);

        // Then
        assertNotNull(predicate);
        verify(criteriaBuilder).like(any(), eq("%123%"));
        verify(criteriaBuilder).like(any(), eq("%test@example.com%"));
    }

    @Test
    void testTransformerWithInvalidValue() {
        // Given
        String rsqlQuery = "number=like='abc'";
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(root.get("number")).thenReturn(mock());
        when(criteriaBuilder.like(any(), eq("%%"))).thenReturn(mock());

        // When
        Node rootNode = new RSQLParser().parse(rsqlQuery);
        RSQLJPAPredicateConverter converter = new RSQLJPAPredicateConverter(criteriaBuilder, propertyPathMapper);
        Predicate predicate = rootNode.accept(converter, root);

        // Then
        assertNotNull(predicate);
        verify(criteriaBuilder).like(any(), eq("%%"));
    }
} 