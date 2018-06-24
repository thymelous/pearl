package org.pearl

import kotlin.test.Test
import kotlin.test.assertEquals

import TestModel
import org.pearl.query.from
import org.pearl.query.updateAll
import org.pearl.query.updateRecord

class UpdateQueryTest {
  @Test
  fun `should produce SQL for individual record updates`() {
    val changeset = Changeset.update(TestModel(id = 2), mapOf("name" to "hhhhh", "enum" to "T3"), listOf("name", "enum"))
    val (sql, bindings) = updateRecord(changeset).toSql(returning = true)

    assertEquals("""UPDATE "TestModel" SET "enum" = ?, "name" = ? WHERE "TestModel"."id" = ? RETURNING *""", sql)
    assertEquals(listOf(TestModel.TestEnum.T3, "hhhhh", 2), bindings)
  }

  @Test
  fun `should produce SQL for predicated updates`() {
    val changeset = Changeset.update(TestModel(), mapOf("name" to "hhhhh"), listOf("name", "enum"))
    val (sql, bindings) = updateAll(changeset).where { it["id"] `in` from<TestModel>().where { it["name"] eq "g" }.select("id") }.toSql(returning = false)

    assertEquals("""UPDATE "TestModel" SET "name" = ? WHERE "TestModel"."id" IN (SELECT "TestModel"."id" FROM "TestModel" WHERE "TestModel"."name" = ?)""", sql)
    assertEquals(listOf("hhhhh", "g"), bindings)
  }
}
