package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Person

interface IPersonRepository {

    fun getAll(): Result<List<Person>>
    fun getAllSortedBy(selector: (Person) -> String?): Result<List<Person>>
    fun getWhere(predicate: (Person) -> Boolean): Result<List<Person>>
    fun findById(id: String): Result<Person?>
    fun findBy(predicate: (Person) -> Boolean): Result<Person?>

    fun create(person: Person): Result<Unit>
    fun update(person: Person): Result<Unit>
    fun remove(person: Person): Result<Unit>


}