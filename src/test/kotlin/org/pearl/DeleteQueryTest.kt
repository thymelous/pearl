package org.pearl

import kotlin.test.Test
import kotlin.test.assertEquals

import TestModel
import org.pearl.query.*

class DeleteQueryTest {
  @Test
  fun `should produce SQL for individual record deletions`() {
    val (sql, bindings) = deleteRecord(TestModel(id = 2)).toSql(returning = false)

    assertEquals("""DELETE FROM "TestModel" WHERE "TestModel"."id" = ?""", sql)
    assertEquals(listOf(2), bindings)
  }

  @Test
  fun `should produce SQL for predicated deletions`() {
    val (sql, bindings) = delete<TestModel>().where { it["name"] `in` from<TestModel>().select("enum") }.toSql(returning = true)

    assertEquals("""DELETE FROM "TestModel" WHERE "TestModel"."name" IN (SELECT "TestModel"."enum" FROM "TestModel") RETURNING *""", sql)
    assertEquals(listOf(), bindings)
  }
}
