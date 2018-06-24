package org.pearl.query

import org.pearl.repo.ParameterizedSql

interface Query<T> {
  fun where(expr: (T) -> WherePredicate): Query<T>

  fun toSql(returning: Boolean = true): ParameterizedSql
}
