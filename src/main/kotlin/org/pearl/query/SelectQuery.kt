package org.pearl.query

import org.pearl.Model
import org.pearl.Sql
import org.pearl.repo.ParameterizedSql
import kotlin.reflect.full.createInstance

inline fun <reified T : Model> from() = SelectQuery(T::class.createInstance())

class SelectQuery<T : Model>(
  val model: T,
  val predicate: WherePredicate? = null,
  val selectList: List<String>? = null,
  val limit: Int? = null
): Query<T> {
  fun select(vararg columns: String) = SelectQuery(model, predicate, columns.toList(), limit)

  /* See `WherePredicate.Column` for how predicates are built */
  override fun where(expr: (T) -> WherePredicate) = SelectQuery(model, predicate?.and(expr(model)) ?: expr(model), selectList, limit)

  fun limit(by: Int) = SelectQuery(model, predicate, selectList, by)

  override fun toSql(returning: Boolean): ParameterizedSql {
    val table = Sql.ident(model.tableName)
    val projection = selectList?.joinToString(", ") { "$table.\"$it\"" } ?: "*"
    val selection = predicate?.let { " WHERE $it" } ?: ""
    val limit = limit?.let { " LIMIT $it" } ?: ""
    val bindings = predicate?.bindings ?: emptyList()

    return Pair("SELECT $projection FROM $table" + selection + limit, bindings)
  }
}
