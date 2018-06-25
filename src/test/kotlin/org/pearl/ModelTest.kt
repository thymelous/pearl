package org.pearl

import TestModel
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {
  class TestModelNameOverride: Model() {
    override val tableName = "h"
  }

  @Test
  fun `should generate table name from class name`() {
    val c = TestModel::class
    assertEquals("TestModel", TestModel().tableName)
    assertEquals("h", TestModelNameOverride().tableName)
  }
}
