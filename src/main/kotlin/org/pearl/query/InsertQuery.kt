package org.pearl.query

import org.pearl.Changeset
import org.pearl.Model
import org.pearl.Sql
import org.pearl.repo.ParameterizedSql

fun <T : Model> insert(changeset: Changeset<T>) = InsertQuery(changeset)

class InsertQuery<T : Model>(
  val changeset: Changeset<T>,
  val predicate: WherePredicate? = null
) : Query<T> {
  override fun where(expr: (T) -> WherePredicate) =
    InsertQuery(changeset, predicate?.and(expr(changeset.record)) ?: expr(changeset.record))

  override fun toSql(returning: Boolean): ParameterizedSql {
    val table = Sql.ident(changeset.record.tableName)
    val columns = changeset.changes.keys.joinToString(", ", transform = Sql::ident)
    val valPlaceholders = Array(changeset.changes.size, { "?" }).joinToString(", ")
    val values = predicate?.let { "SELECT $valPlaceholders FROM $table WHERE $it" } ?: "VALUES ($valPlaceholders)"
    val returningClause = if (returning) " RETURNING *" else ""
    val bindings = predicate?.bindings?.let { changeset.changes.values.toList() + it } ?: changeset.changes.values.toList()

    return Pair("INSERT INTO $table ($columns) $values" + returningClause, bindings)
  }
}
