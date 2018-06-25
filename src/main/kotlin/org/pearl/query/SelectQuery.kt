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
  val orderClauses: List<Pair<String, SortDirection>>? = null,
  val limit: Int? = null
): Query<T> {
  fun select(vararg columns: String) =
    SelectQuery(model, predicate, columns.toList(), orderClauses, limit)

  /* See `WherePredicate.Column` for how predicates are built */
  override fun where(expr: (T) -> WherePredicate) =
    SelectQuery(model, predicate?.and(expr(model)) ?: expr(model), selectList, orderClauses, limit)

  enum class SortDirection { ASC, DESC }

  fun orderBy(column: String, direction: SortDirection = SortDirection.ASC) =
    SelectQuery(model, predicate, selectList, orderClauses.orEmpty() + Pair(column, direction), limit)

  fun limit(by: Int) =
    SelectQuery(model, predicate, selectList, orderClauses, by)

  override fun toSql(returning: Boolean): ParameterizedSql {
    val table = Sql.ident(model.tableName)
    val projection = selectList?.joinToString(", ") { "$table.\"$it\"" } ?: "*"
    val selection = predicate?.let { " WHERE $it" } ?: ""
    val sort = orderClauses?.joinToString(", ") { "${Sql.ident(it.first)} ${it.second}" }?.let { " ORDER BY $it" } ?: ""
    val limit = limit?.let { " LIMIT $it" } ?: ""
    val bindings = predicate?.bindings ?: emptyList()

    return Pair("SELECT $projection FROM $table" + selection + sort + limit, bindings)
  }
}
