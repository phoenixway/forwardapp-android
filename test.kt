package com.example.test

import java.util.Random

/**
 * Це тестовий клас для перевірки скрипта Smart Paste.
 * Спробуйте різні сценарії заміни на цьому файлі.
 */
class UserManager {

    private val users = mutableListOf("Alice", "Bob", "Charlie")

    // --- СЦЕНАРІЙ 1: SMART REPLACE ---
    // Скопіюйте "Нову версію" методу updateUser (знизу) у буфер.
    // Поставте курсор будь-де всередині цього методу і запустіть скрипт.
    // Скрипт має знайти цей метод за назвою і замінити його повністю.
    fun updateUser(id: Int, newName: String) {
        println("Old Logic: Updating user $id to $newName")
        if (id >= 0 && id < users.size) {
            users[id] = newName
        } else {
            println("Error: User not found")
        }
    }

    // --- СЦЕНАРІЙ 2: SELECTION REPLACE ---
    // Виділіть вміст циклу for (рядки 26-28).
    // Скопіюйте якийсь сніпет (наприклад: println("Filtered: $user"))
    // Запустіть скрипт. Він має замінити тільки виділене.
    fun processList() {
        println("Starting processing...")
        for (user in users) {
            if (user.startsWith("A")) {
                println("Found user starting with A: $user")
            }
        }
        println("Done.")
    }

    fun deleteUser(id: Int) {
        // Цей метод можна спробувати видалити або замінити
        users.removeAt(id)
    }
}

// --- СЦЕНАРІЙ 3: INSERTION ---
// Скопіюйте новий метод (наприклад fun logAnalytics()) у буфер.
// Поставте курсор на пустий рядок тут і запустіть скрипт.
fun utilFunction() {
    val random = Random()
    println("Random number: ${random.nextInt()}")
}
