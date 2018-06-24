package org.pearl.query

import org.pearl.Changeset
import org.pearl.Model
import org.pearl.Sql
import org.pearl.reflection.propertyValue
import org.pearl.repo.ParameterizedSql

inline fun <reified T : Model> updateRecord(changeset: Changeset<T>) =
  changeset.record.schema.entries
    .find { (_, col) -> col.isPrimaryKey }
    ?.let { (key, _) -> UpdateQuery(changeset, changeset.record[key] eq changeset.record.propertyValue(key)!!) }
    ?: throw IllegalArgumentException("The model associated with the changeset has no primary key column.")

fun <T : Model> updateAll(changeset: Changeset<T>) = UpdateQuery(changeset)

class UpdateQuery<T : Model>(
  val changeset: Changeset<T>,
  val predicate: WherePredicate? = null
) : Query<T> {
  override fun where(expr: (T) -> WherePredicate) =
    UpdateQuery(changeset, predicate?.and(expr(changeset.record)) ?: expr(changeset.record))

  override fun toSql(returning: Boolean): ParameterizedSql {
    val updatedFields = changeset.changes.keys.joinToString(", ") { "${Sql.ident(it)} = ?" }
    val selection = predicate?.let { " WHERE $it" } ?: ""
    val bindings = changeset.changes.values + (predicate?.bindings ?: emptyList())
    val returningClause = if (returning) " RETURNING *" else ""

    return Pair("UPDATE ${Sql.ident(changeset.record.tableName)} SET $updatedFields" + selection + returningClause, bindings)
  }
}
