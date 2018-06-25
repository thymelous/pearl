package org.pearl

import TestModel
import org.pearl.query.delete
import org.pearl.query.deleteRecord
import org.pearl.query.from
import kotlin.test.Test
import kotlin.test.assertEquals

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
