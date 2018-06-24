package org.pearl

import org.pearl.repo.ParameterizedSql

object Sql {
  @JvmStatic
  fun tableDefinition(model: Model) =
    "CREATE TABLE ${ident(model.tableName)} (${tableColumns(model)})"

  @JvmStatic
  fun insert(changeset: Changeset<*>): ParameterizedSql =
    Pair("INSERT INTO ${ident(changeset.record.tableName)} " +
      "(${changeset.changes.keys.joinToString(", ", transform = Sql::ident)}) " +
      "VALUES (${Array(changeset.changes.size, { "?" }).joinToString(", ")}) RETURNING *", changeset.changes.values.toList())

  @JvmStatic
  private fun tableColumns(model: Model) =
    model.schema.entries.joinToString(", ") {
      ident(it.key) + ' ' + it.value.sqlType +
        when {
          it.value.isPrimaryKey -> " PRIMARY KEY"
          !it.value.isNullable -> " NOT NULL"
          else -> ""
        }
    }

  @JvmStatic
  fun ident(unescapedName: String) = '"' + unescapedName.replace("\"", "\\\"") + '"'
}
