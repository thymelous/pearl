package org.pearl

import kotlin.test.Test

import TestModel
import org.pearl.query.from
import org.pearl.query.updateAll
import org.pearl.query.updateRecord
import org.pearl.repo.Repo
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class RepoTest {
  @BeforeTest
  fun init() {
    Repo.connect("localhost", 5432, dbname = "pearl", username = "pearl", password = "pearl")

    Repo.rawSqlUpdate("""DROP TABLE IF EXISTS "TestModel"""")
  }

  @Test
  fun `should create tables`() {
    Repo.createTable<TestModel>()
    assertEquals(emptyList(), Repo.many(from<TestModel>()))
  }

  @Test
  fun `should insert new records`() {
    Repo.createTable<TestModel>()
    assertEquals(1, Repo.insert(Changeset.newRecord<TestModel>(
      mapOf("name" to "aaa", "size" to "100", "enum" to "T2"), listOf("name", "size", "enum"))).id)
    assertEquals(2, Repo.insert(Changeset.newRecord<TestModel>(
      mapOf("name" to "bbb", "size" to "120", "enum" to "T3"), listOf("name", "size", "enum"))).id)

    assertEquals(listOf(1, 2), Repo.many(from<TestModel>().where { it["size"] lt 200 }).map { it.id })
    assertEquals(listOf(1), Repo.many(from<TestModel>().where { it["name"] eq "aaa" }).map { it.id })
    assertEquals(listOf(2), Repo.many(from<TestModel>().where { it["enum"] eq TestModel.TestEnum.T3 }).map { it.id })

    /* Individual selects */
    assertEquals("[[1, T2], [2, T3]]", Repo.rows(from<TestModel>().select("id", "enum")).toString())
  }

  @Test
  fun `should update existing records`() {
    Repo.createTable<TestModel>()

    val record = Repo.insert(Changeset.newRecord<TestModel>(
      mapOf("name" to "aaa", "size" to "100", "enum" to "T2"), listOf("name", "size", "enum")))
    var changeset = Changeset.update(record, mapOf("enum" to "T3", "size" to "300"), listOf("size"))
    val updated = Repo.one(updateRecord(changeset))

    assertEquals(record.let { (id, name, date, _, enum) -> TestModel(id, name, date, size = 300, enum = enum) }, updated)

    Repo.insert(Changeset.newRecord<TestModel>(mapOf("name" to "bbb", "size" to "200"), listOf("name", "size")))
    Repo.insert(Changeset.newRecord<TestModel>(mapOf("name" to "ccc", "size" to "300"), listOf("name", "size")))

    changeset = Changeset.update(TestModel(), mapOf("size" to "100"), listOf("size"))
    val updatedRecords = Repo.many(updateAll(changeset))

    assertEquals(listOf(100, 100, 100), updatedRecords.map { it.size })

    Repo.execute(updateAll(Changeset.update(TestModel(), mapOf("enum" to "T3"), listOf("enum"))))

    assertEquals(listOf(1, 2, 3), Repo.many(from<TestModel>().where { it["enum"] eq TestModel.TestEnum.T3 }).map { it.id })
  }
}
