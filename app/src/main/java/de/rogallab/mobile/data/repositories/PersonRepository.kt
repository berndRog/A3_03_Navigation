package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person

class PersonRepository(
   private val _dataStore: IDataStore
) : IPersonRepository {
   override fun getAll(): Result<List<Person>> =
      try {
         Result.success(_dataStore.selectAll())
      } catch (e: Exception) {
         Result.failure(e)
      }

   override fun getAllSortedBy(
      selector: (Person) -> String?
   ): Result<List<Person>> =
      try {
         Result.success(_dataStore.selectAllSortedBy(selector))
      } catch (e: Throwable) {
         Result.failure(e)
      }

   override fun getWhere(
      predicate: (Person) -> Boolean
   ): Result<List<Person>> =
      try {
         Result.success(_dataStore.selectWhere(predicate))
      } catch (e: Throwable) {
         Result.failure(e)
      }

   override fun findById(id: String): Result<Person?> =
      try {
         Result.success(_dataStore.findById(id))
      } catch (e: Exception) {
         Result.failure(e)
      }

   override fun findBy(
      predicate: (Person) -> Boolean
   ): Result<Person?> =
      try {
         Result.success(_dataStore.findBy(predicate))
      } catch (e: Exception) {
         Result.failure(e)
      }

   override fun create(person: Person): Result<Unit> =
      try {
         _dataStore.insert(person)
         Result.success(Unit)
      } catch (e: Exception) {
         Result.failure(e)
      }

   override fun update(person: Person): Result<Unit> =
      try {
         _dataStore.update(person)
         Result.success(Unit)
      } catch (e: Exception) {
         Result.failure(e)
      }

   override fun remove(person: Person): Result<Unit> =
      try {
         _dataStore.delete(person)
         Result.success(Unit)
      } catch (e: Exception) {
         Result.failure(e)
      }
}