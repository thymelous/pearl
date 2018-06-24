package org.pearl.query

import org.pearl.Model
import org.pearl.Sql
import org.pearl.reflection.propertyValue
import org.pearl.repo.ParameterizedSql
import kotlin.reflect.full.createInstance

inline fun <reified T : Model> deleteRecord(record: T) =
  record.schema.entries
    .find { (_, col) -> col.isPrimaryKey }
    ?.let { (key, _) -> DeleteQuery(record, record[key] eq record.propertyValue(key)!!) }
    ?: throw IllegalArgumentException("The model associated with the changeset has no primary key column.")

inline fun <reified T : Model> delete() = DeleteQuery(T::class.createInstance())

class DeleteQuery<T : Model>(
  val record: T,
  val predicate: WherePredicate? = null
) : Query<T> {
  override fun where(expr: (T) -> WherePredicate) =
    DeleteQuery(record, predicate?.and(expr(record)) ?: expr(record))

  override fun toSql(returning: Boolean): ParameterizedSql {
    val selection = predicate?.let { " WHERE $it" } ?: ""
    val bindings = predicate?.bindings ?: emptyList()
    val returningClause = if (returning) " RETURNING *" else ""

    return Pair("DELETE FROM ${Sql.ident(record.tableName)}" + selection + returningClause, bindings)
  }
}
