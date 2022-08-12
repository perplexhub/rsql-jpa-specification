package io.github.perplexhub.rsql;

public class Q {

    public static QuerySupport.QuerySupportBuilder rsql(String rsqlQuery) {
        return QuerySupport.builder().rsqlQuery(rsqlQuery);
    }

}
