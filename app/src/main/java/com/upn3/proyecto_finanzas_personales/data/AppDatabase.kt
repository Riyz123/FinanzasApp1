package com.upn3.proyecto_finanzas_personales.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.upn3.proyecto_finanzas_personales.model.AuditLogEntity
import com.upn3.proyecto_finanzas_personales.model.BudgetEntity
import com.upn3.proyecto_finanzas_personales.model.FixedExpenseEntity
import com.upn3.proyecto_finanzas_personales.model.SavingsGoalEntity
import com.upn3.proyecto_finanzas_personales.model.TransactionEntity
import com.upn3.proyecto_finanzas_personales.model.User

@Database(
    entities = [
        TransactionEntity::class,
        User::class,
        AuditLogEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        FixedExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun planningDao(): PlanningDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN lastModified INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiptPath TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recipientName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recipientAlias TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recipientBank TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferMotivo TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `id` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `userEmail` TEXT NOT NULL,
                        `changedFieldsJson` TEXT NOT NULL DEFAULT '{}',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` TEXT NOT NULL,
                        `walletId` TEXT NOT NULL DEFAULT '',
                        `categoryName` TEXT NOT NULL DEFAULT '',
                        `limitAmount` REAL NOT NULL DEFAULT 0.0,
                        `period` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `currencyCode` TEXT NOT NULL DEFAULT 'PEN',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `targetAmount` REAL NOT NULL DEFAULT 0.0,
                        `currentAmount` REAL NOT NULL DEFAULT 0.0,
                        `deadline` INTEGER NOT NULL DEFAULT 0,
                        `currencyCode` TEXT NOT NULL DEFAULT 'PEN',
                        `color` TEXT NOT NULL DEFAULT '#4338CA',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_expenses` (
                        `id` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `amount` REAL NOT NULL DEFAULT 0.0,
                        `categoryName` TEXT NOT NULL DEFAULT '',
                        `walletId` TEXT NOT NULL DEFAULT '',
                        `dayOfMonth` INTEGER NOT NULL DEFAULT 1,
                        `currencyCode` TEXT NOT NULL DEFAULT 'PEN',
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzas_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
