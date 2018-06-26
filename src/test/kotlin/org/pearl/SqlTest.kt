package org.pearl

import Consts.defaultDate
import Consts.defaultZonedDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlTest {
  data class SqlTestModel(
    @Id val id: Int = 0,
    val name: String = "",
    val double: Double = 1.1,
    val date: LocalDateTime = defaultDate,
    val zonedDate: ZonedDateTime = defaultZonedDate,
    val enum: SampleEnum = SampleEnum.VAL1
  ): Model() {
    enum class SampleEnum { VAL1, VAL2 }
  }

  @Test
  fun `should generate table definitions`() {
    assertEquals("""CREATE TABLE "SqlTestModel"
      | ("date" timestamp NOT NULL,
      | "double" double precision NOT NULL,
      | "enum" text NOT NULL,
      | "id" serial PRIMARY KEY,
      | "name" text NOT NULL,
      | "zonedDate" timestamptz NOT NULL)""".trimMargin().replace("\n", ""),
      Sql.tableDefinition(SqlTestModel()))
  }
}
