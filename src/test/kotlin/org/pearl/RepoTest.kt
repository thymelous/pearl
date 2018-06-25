package org.pearl

import TestModel
import org.pearl.query.*
import org.pearl.repo.Repo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepoTest {
  @BeforeTest
  fun init() {
    Repo.connect("localhost", 5432, dbname = "pearl", username = "pearl", password = "pearl")

    Repo.rawSqlUpdate("""DROP TABLE IF EXISTS "TestModel"""")
    Repo.createTable<TestModel>()
  }

  @Test
  fun `should insert new records`() {
    assertEquals(1, Repo.one(insert(Changeset.newRecord<TestModel>(
      mapOf("name" to "aaa", "size" to "100", "enum" to "T2"), listOf("name", "size", "enum"))))?.id)
    assertEquals(2, Repo.one(insert(Changeset.newRecord(TestModel(name = "bbb", size = 120, enum = TestModel.TestEnum.T3))))?.id)

    assertEquals(listOf(1, 2), Repo.many(from<TestModel>().where { it["size"] lt 200 }).map { it.id })
    assertEquals(listOf(1), Repo.many(from<TestModel>().where { it["name"] eq "aaa" }).map { it.id })
    assertEquals(listOf(2), Repo.many(from<TestModel>().where { it["enum"] eq TestModel.TestEnum.T3 }).map { it.id })

    /* Individual selects */
    assertEquals("[[1, T2], [2, T3]]", Repo.rows(from<TestModel>().select("id", "enum")).toString())

    /* Conditional inserts */
    assertNull(Repo.one(insert(Changeset.newRecord(TestModel())).where { not(exists(from<TestModel>())) }))
    assertEquals(3, Repo.one(insert(Changeset.newRecord(TestModel())).where { not(exists(from<TestModel>().where { it["name"] eq "defnothere" })) })?.id)
  }

  @Test
  fun `should update existing records`() {
    val record = Repo.one(insert(Changeset.newRecord<TestModel>(
      mapOf("name" to "aaa", "size" to "100", "enum" to "T2"), listOf("name", "size", "enum"))))!!
    var changeset = Changeset.update(record, mapOf("enum" to "T3", "size" to "300"), listOf("size"))
    val updated = Repo.one(updateRecord(changeset))!!

    assertEquals(record.let { (id, name, date, _, enum) -> TestModel(id, name, date, size = 300, enum = enum) }, updated)

    Repo.execute(insert(Changeset.newRecord(TestModel(name = "bbb", size = 200))))
    Repo.execute(insert(Changeset.newRecord(TestModel(name = "ccc", size = 300))))

    changeset = Changeset.update(TestModel(), mapOf("size" to "100"), listOf("size"))
    val updatedRecords = Repo.many(updateAll(changeset))

    assertEquals(listOf(100, 100, 100), updatedRecords.map { it.size })

    Repo.execute(updateAll(Changeset.update(TestModel(), mapOf("enum" to "T3"), listOf("enum"))))

    assertEquals(listOf(1, 2, 3), Repo.many(from<TestModel>().where { it["enum"] eq TestModel.TestEnum.T3 }).map { it.id })

    Repo.execute(updateAll(Changeset.update(TestModel(), TestModel(size = 1))))

    assertEquals(listOf(1, 2, 3), Repo.many(from<TestModel>().where { it["size"] eq 1 }).map { it.id })
  }

  @Test
  fun `should remove records`() {
    listOf("hey", "you", "out", "there", "in", "the", "cold").forEach {
      Repo.execute(insert(Changeset.newRecord(TestModel(name = it))))
    }

    var deleted = Repo.many(delete<TestModel>().where { it["name"] gt "s" })
    assertEquals(listOf(2, 4, 6), deleted.map { it.id })
    assertEquals(4, Repo.many(from<TestModel>()).size)

    Repo.execute(deleteRecord(TestModel(id = 1)))
    assertEquals(3, Repo.many(from<TestModel>()).size)

    deleted = Repo.many(delete<TestModel>().where {
      it["id"] `in` from<TestModel>().select("id").orderBy("name", SelectQuery.SortDirection.DESC).limit(1) })
    assertEquals(listOf(3), deleted.map { it.id })

    Repo.execute(delete<TestModel>())
    assertEquals(0, Repo.many(from<TestModel>()).size)
  }
}
