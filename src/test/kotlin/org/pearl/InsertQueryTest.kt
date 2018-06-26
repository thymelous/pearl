package org.pearl

import Consts.defaultDate
import TestModel
import org.pearl.query.*
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertQueryTest {
  @Test
  fun `should produce SQL for unconditional record insertions`() {
    val changeset = Changeset.newRecord<TestModel>(
      params = mapOf("name" to "hey", "size" to "30", "enum" to "T1"),
      allowedParams = listOf("name", "size", "enum")
    )
    val (sql, bindings) = insert(changeset).toSql(returning = false)

    assertEquals("""INSERT INTO "TestModel" ("date", "enum", "name", "size") VALUES (?, ?, ?, ?)""", sql)
    assertEquals(listOf(defaultDate, TestModel.TestEnum.T1, "hey", 30), bindings)
  }

  @Test
  fun `should produce SQL for predicated record insertions`() {
    val changeset = Changeset.newRecord(TestModel(name = "hey", size = 30, enum = TestModel.TestEnum.T1))
    val (sql, bindings) = insert(changeset).where { not(exists(from<TestModel>().where { it["size"] gt 50 })) }.toSql(returning = false)

    assertEquals("""INSERT INTO "TestModel" ("date", "enum", "name", "size") """ +
      """SELECT ?, ?, ?, ? FROM "TestModel" WHERE NOT EXISTS (SELECT * FROM "TestModel" WHERE "TestModel"."size" > ?) LIMIT 1""", sql)
    assertEquals(listOf(defaultDate, TestModel.TestEnum.T1, "hey", 30, 50), bindings)
  }
}
