package com.putracode.utils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;

/**
 * Created by KrisnaPutra on 5/23/2016.
 */
public class RsqlSpecification {
    public static <T> Specification<T> rsql(final String rsqlQuery) {
        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
               if(StringUtils.hasText(rsqlQuery)){
                   Node rsql =new RSQLParser().parse(rsqlQuery);
                   return rsql.accept(new JPARsqlConverter(cb), root);
               }else
                   return null;
            }
        };
    }
}
