package org.pearl

import kotlin.reflect.full.createInstance
import org.pearl.Schemas.Column.SqlType.Primitive
import org.pearl.Schemas.Column.SqlType.Primitive.PrimitiveMapping.*
import org.pearl.Schemas.Column.SqlType.EnumStringified
import org.pearl.reflection.hasAnnotation
import org.pearl.reflection.property
import org.pearl.reflection.propertyValue

data class Changeset<T : Model>(val record: T, val changes: Map<String, Any?>, val errors: List<String>) {
  companion object {
    inline fun <reified T : Model> newRecord(params: Map<String, String>, allowedParams: List<String>): Changeset<T> =
      assign(T::class.createInstance(), params, allowedParams)

    inline fun <reified T : Model> newRecord(record: T): Changeset<T> =
      Changeset(record, errors = emptyList(),
        changes = record.schema.keys
          .filter { name -> record.property(name)?.hasAnnotation<Id>() == false }
          .map { name -> Pair(name, record.propertyValue(name)) }.toMap())

    inline fun <reified T : Model> update(record: T, params: Map<String, String>, allowedParams: List<String>): Changeset<T> =
      assign(record, params, allowedParams).let { (record, changes, errors) ->
        Changeset(record, changes.filter { it.value != record.propertyValue(it.key) }, errors)
      }

    inline fun <reified T : Model> update(oldRecord: T, newRecord: T): Changeset<T> =
      oldRecord.schema.keys
        .fold(mapOf<String, Any?>()) { changes, name ->
          val oldValue = oldRecord.propertyValue(name)
          val newValue = newRecord.propertyValue(name)

          if (oldValue != newValue) changes + Pair(name, newValue) else changes
        }
        .let { changes ->
          Changeset(oldRecord, changes, emptyList())
        }

    inline fun <reified T : Model> assign(record: T, params: Map<String, String>, allowedParams: List<String>): Changeset<T> {
      val (changes, errors) = cast(record.schema, record::propertyValue, params.filterKeys(allowedParams::contains))
      return Changeset(record, changes, errors)
    }

    fun cast(schema: Schema, defaultLookup: (String) -> Any?, params: Map<String, String>): Pair<Map<String, Any?>, List<String>> =
      schema.entries.fold(Pair(mapOf(), listOf())) { (changes, errors), (name, column) ->
        val param = params[name]
        when {
          param.isNullOrEmpty() && column.isNullable && column.sqlType != Primitive(STRING) ->
            Pair(changes + Pair(name, null), errors)
          param == null && column.isNullable ->
            Pair(changes + Pair(name, null), errors)
          param == null && !column.isPrimaryKey ->
            Pair(changes + Pair(name, defaultLookup(name)), errors)
          param != null && column.sqlType is EnumStringified ->
            column.sqlType.enumConstants
              .find { it.name == param }
              ?.let { Pair(changes + Pair(name, it), errors) }
              ?: Pair(changes, errors + "Incorrect value provided for \"$name\"")
          param != null && column.sqlType is Primitive ->
            try { Pair(changes + Pair(name, column.sqlType.mapping.cast(param)), errors) }
            catch (_: Exception) { Pair(changes, errors + "Incorrect value provided for \"$name\"") }
          else ->
            Pair(changes, errors)
        }
      }
  }

  inline fun <T> validate(column: String, validator: (T?) -> Boolean, message: String) =
    if (validator(changes[column] as T?)) this
    else Changeset(record, changes, errors + message)
}
