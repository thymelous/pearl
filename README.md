# Pearl

[![Build Status](https://travis-ci.org/thymelous/pearl.svg?branch=master)](https://travis-ci.org/thymelous/pearl)
[![codecov](https://codecov.io/gh/thymelous/pearl/branch/master/graph/badge.svg)](https://codecov.io/gh/thymelous/pearl)

A toy data mapper I've build as an assignment for Java programming course.

It is heavily inspired by Ecto, and I purposefully attempted
to steer away from the ActiveRecord/Hibernate way of managing persistence, because
I've been burned by it in the past.

Interactions with the database are explicit and performed strictly through the
repository (`Repo`). The records are represented by immutable `data` classes,
creation and updates are handled through changesets.

## Example usage

As mentioned above, this is not really intended to be used anywhere, but I've
still compiled basic usage instructions to give a more concrete overview of
how the library functions.

The dependency is `com.github.thymelous:pearl:master-SNAPSHOT`.

Connecting to a database (PostgreSQL 9.6+) is as simple as
```kotlin
import org.pearl.repo.Repo

Repo.connect("localhost", 5432, dbname = "...", username = "...", password = "...")
```
This needs to be done once, at application startup.

### Models

A model is just a data class with constant (`val`) properties
inherited from `org.pearl.Model`. It must have a primary key property
(either `Int` or `Long`, annotated with `Id`) and have a default constructor
without parameters.
 
Currently supported datatypes are `Int`, `Long`, `Double`, `String`,
`LocalDateTime`, and `ZonedDateTime`. Enums are also supported out-of-the-box:
you don't need to specify anything, they are automatically converted to and from
their `String` representations.

### Data manipulation

Records are created through changesets:

```kotlin
val changeset = Changeset.newRecord(Image(url = "img_url", size = Enum.SMALL))
// Alternatively, from a map of param name (String) to value (String):
val paramChangeset = Changeset.newRecord<Image>(
  params = mapOf("url" to "img_url", "size" to "SMALL", "secret" to "forbidden"),
  allowedParms = listOf("name", "size"))
// Note that since "secret" is not an allowed param, its value will be defaulted

val inserted = Repo.insert(changeset)
inserted.id
// => 1
```

Individual updates are handled similarly:
```kotlin
val paramChangest = Changeset.update(record, mapOf("size" to "LARGE"), listOf("size"))
// Returning updated record
val updated = Repo.one(updateRecord(paramChangest))
// Just updating
Repo.execute(updateRecord(paramChangest))
```

Deletions are performed using the primary key only:
```kotlin
val toBeDeleted = Image(...)
// Assuming toBeDeleted's id = 4, the following two commands are equivalent:
Repo.execute(deleteRecord(toBeDeleted))
Repo.execute(deleteRecord(Image(id = 4)))
```

### Queries

When it comes to querying data, Pearl has a (somewhat limited) DSL that supports both record instantiation
and projection (selecting a subset of columns).

```kotlin
val record = Repo.one(from<Image>().where { it["id"] eq 1 })
val records = Repo.many(from<Image>().where { it["size"] eq Enum.LARGE })
val projected = Repo.rows(from<Image>().where { (it["width"] lt 200) and (it["height"] gt 100) }.select("id", "size"))
```

A nifty feature is the support for predicates when updating and deleting data:
```kotlin
// Set "hasPreview" to true for all records with "size" = LARGE or "width" more than 800
Repo.execute(updateAll(Changeset.update(Image(), Image(hasPreview = true))).where { (it["size"] eq Enum.LARGE) or (it["width"] gt 800) })
// Delete all images disliked by at least one administrator and return them
val removedRecords = Repo.many(delete<Image>().where { it["dislikedByUser"] `in` from<User>().select("name").where { it["role"] eq "admin" })
// Only delete the record if its "size" is null or "deletedOn' is not null
Repo.execute(deleteRecord(record).where { it["size"].isNull() or it["deletedOn"].isNotNull() })
``` 
