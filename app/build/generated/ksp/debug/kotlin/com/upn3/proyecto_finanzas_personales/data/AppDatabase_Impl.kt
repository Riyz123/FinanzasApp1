package com.upn3.proyecto_finanzas_personales.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _transactionDao: Lazy<TransactionDao> = lazy {
    TransactionDao_Impl(this)
  }


  private val _auditLogDao: Lazy<AuditLogDao> = lazy {
    AuditLogDao_Impl(this)
  }


  private val _planningDao: Lazy<PlanningDao> = lazy {
    PlanningDao_Impl(this)
  }


  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "b0e385809b5de1f357bacb58869e8ad1", "2cb721b33e7e5cfd87fd847354db2f2d") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `userEmail` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `origin` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `lastModified` INTEGER, `receiptPath` TEXT, `recipientName` TEXT, `recipientAlias` TEXT, `recipientBank` TEXT, `transferMotivo` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `users` (`email` TEXT NOT NULL, `password` TEXT NOT NULL, `name` TEXT NOT NULL, `lastname` TEXT NOT NULL, `theme` TEXT NOT NULL, `profilePicture` TEXT NOT NULL, PRIMARY KEY(`email`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `transactionId` TEXT NOT NULL, `action` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `userEmail` TEXT NOT NULL, `changedFieldsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `walletId` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `limitAmount` REAL NOT NULL, `period` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `savings_goals` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `deadline` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `color` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `fixed_expenses` (`id` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, `categoryName` TEXT NOT NULL, `walletId` TEXT NOT NULL, `dayOfMonth` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b0e385809b5de1f357bacb58869e8ad1')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transactions`")
        connection.execSQL("DROP TABLE IF EXISTS `users`")
        connection.execSQL("DROP TABLE IF EXISTS `audit_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `budgets`")
        connection.execSQL("DROP TABLE IF EXISTS `savings_goals`")
        connection.execSQL("DROP TABLE IF EXISTS `fixed_expenses`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactions.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("userEmail", TableInfo.Column("userEmail", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("origin", TableInfo.Column("origin", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("latitude", TableInfo.Column("latitude", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("longitude", TableInfo.Column("longitude", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("lastModified", TableInfo.Column("lastModified", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("receiptPath", TableInfo.Column("receiptPath", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("recipientName", TableInfo.Column("recipientName", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("recipientAlias", TableInfo.Column("recipientAlias", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("recipientBank", TableInfo.Column("recipientBank", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("transferMotivo", TableInfo.Column("transferMotivo", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTransactions: TableInfo = TableInfo("transactions", _columnsTransactions,
            _foreignKeysTransactions, _indicesTransactions)
        val _existingTransactions: TableInfo = read(connection, "transactions")
        if (!_infoTransactions.equals(_existingTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transactions(com.upn3.proyecto_finanzas_personales.model.TransactionEntity).
              | Expected:
              |""".trimMargin() + _infoTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingTransactions)
        }
        val _columnsUsers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUsers.put("email", TableInfo.Column("email", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("password", TableInfo.Column("password", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("lastname", TableInfo.Column("lastname", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("theme", TableInfo.Column("theme", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("profilePicture", TableInfo.Column("profilePicture", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUsers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers,
            _indicesUsers)
        val _existingUsers: TableInfo = read(connection, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |users(com.upn3.proyecto_finanzas_personales.model.User).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsAuditLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAuditLogs.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("transactionId", TableInfo.Column("transactionId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("action", TableInfo.Column("action", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("userEmail", TableInfo.Column("userEmail", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("changedFieldsJson", TableInfo.Column("changedFieldsJson", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAuditLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAuditLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAuditLogs: TableInfo = TableInfo("audit_logs", _columnsAuditLogs,
            _foreignKeysAuditLogs, _indicesAuditLogs)
        val _existingAuditLogs: TableInfo = read(connection, "audit_logs")
        if (!_infoAuditLogs.equals(_existingAuditLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |audit_logs(com.upn3.proyecto_finanzas_personales.model.AuditLogEntity).
              | Expected:
              |""".trimMargin() + _infoAuditLogs + """
              |
              | Found:
              |""".trimMargin() + _existingAuditLogs)
        }
        val _columnsBudgets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBudgets.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("walletId", TableInfo.Column("walletId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("categoryName", TableInfo.Column("categoryName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("limitAmount", TableInfo.Column("limitAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("period", TableInfo.Column("period", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("currencyCode", TableInfo.Column("currencyCode", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBudgets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBudgets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBudgets: TableInfo = TableInfo("budgets", _columnsBudgets, _foreignKeysBudgets,
            _indicesBudgets)
        val _existingBudgets: TableInfo = read(connection, "budgets")
        if (!_infoBudgets.equals(_existingBudgets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |budgets(com.upn3.proyecto_finanzas_personales.model.BudgetEntity).
              | Expected:
              |""".trimMargin() + _infoBudgets + """
              |
              | Found:
              |""".trimMargin() + _existingBudgets)
        }
        val _columnsSavingsGoals: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSavingsGoals.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("targetAmount", TableInfo.Column("targetAmount", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("currentAmount", TableInfo.Column("currentAmount", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("deadline", TableInfo.Column("deadline", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("currencyCode", TableInfo.Column("currencyCode", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSavingsGoals.put("color", TableInfo.Column("color", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSavingsGoals: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSavingsGoals: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSavingsGoals: TableInfo = TableInfo("savings_goals", _columnsSavingsGoals,
            _foreignKeysSavingsGoals, _indicesSavingsGoals)
        val _existingSavingsGoals: TableInfo = read(connection, "savings_goals")
        if (!_infoSavingsGoals.equals(_existingSavingsGoals)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |savings_goals(com.upn3.proyecto_finanzas_personales.model.SavingsGoalEntity).
              | Expected:
              |""".trimMargin() + _infoSavingsGoals + """
              |
              | Found:
              |""".trimMargin() + _existingSavingsGoals)
        }
        val _columnsFixedExpenses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFixedExpenses.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("categoryName", TableInfo.Column("categoryName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("walletId", TableInfo.Column("walletId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("dayOfMonth", TableInfo.Column("dayOfMonth", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("currencyCode", TableInfo.Column("currencyCode", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFixedExpenses.put("isActive", TableInfo.Column("isActive", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFixedExpenses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFixedExpenses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFixedExpenses: TableInfo = TableInfo("fixed_expenses", _columnsFixedExpenses,
            _foreignKeysFixedExpenses, _indicesFixedExpenses)
        val _existingFixedExpenses: TableInfo = read(connection, "fixed_expenses")
        if (!_infoFixedExpenses.equals(_existingFixedExpenses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |fixed_expenses(com.upn3.proyecto_finanzas_personales.model.FixedExpenseEntity).
              | Expected:
              |""".trimMargin() + _infoFixedExpenses + """
              |
              | Found:
              |""".trimMargin() + _existingFixedExpenses)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions", "users",
        "audit_logs", "budgets", "savings_goals", "fixed_expenses")
  }

  public override fun clearAllTables() {
    super.performClear(false, "transactions", "users", "audit_logs", "budgets", "savings_goals",
        "fixed_expenses")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TransactionDao::class, TransactionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AuditLogDao::class, AuditLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PlanningDao::class, PlanningDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun transactionDao(): TransactionDao = _transactionDao.value

  public override fun auditLogDao(): AuditLogDao = _auditLogDao.value

  public override fun planningDao(): PlanningDao = _planningDao.value
}
