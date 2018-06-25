package org.pearl

import org.pearl.repo.ParameterizedSql

object Sql {
  @JvmStatic
  fun tableDefinition(model: Model) =
    "CREATE TABLE ${ident(model.tableName)} (${tableColumns(model)})"

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
