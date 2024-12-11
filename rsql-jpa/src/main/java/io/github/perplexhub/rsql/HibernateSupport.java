package io.github.perplexhub.rsql;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.util.ClassUtils;

final class HibernateSupport {

  private static final boolean isHibernatePresent = ClassUtils.isPresent(
      "org.hibernate.query.criteria.HibernateCriteriaBuilder", HibernateSupport.class.getClassLoader());

  private HibernateSupport() {
  }

  static boolean isHibernateCriteriaBuilder(CriteriaBuilder cb) {
    return isHibernatePresent && cb instanceof HibernateCriteriaBuilder;
  }

  /**
   * Must be guarded with {@linkplain #isHibernatePresent} before invoking.
   */
  static Predicate ilike(CriteriaBuilder cb, Expression<String> expression, String arg, Character escapeChar) {
    var hcb = (HibernateCriteriaBuilder) cb;
    var pattern = '%' + arg + '%';

    return escapeChar != null
        ? hcb.ilike(expression, pattern, escapeChar)
        : hcb.ilike(expression, pattern);
  }
}
