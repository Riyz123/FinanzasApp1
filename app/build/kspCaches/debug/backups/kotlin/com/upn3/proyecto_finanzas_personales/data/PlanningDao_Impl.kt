package com.upn3.proyecto_finanzas_personales.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.upn3.proyecto_finanzas_personales.model.BudgetEntity
import com.upn3.proyecto_finanzas_personales.model.FixedExpenseEntity
import com.upn3.proyecto_finanzas_personales.model.SavingsGoalEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PlanningDao_Impl(
  __db: RoomDatabase,
) : PlanningDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBudgetEntity: EntityInsertAdapter<BudgetEntity>

  private val __insertAdapterOfSavingsGoalEntity: EntityInsertAdapter<SavingsGoalEntity>

  private val __insertAdapterOfFixedExpenseEntity: EntityInsertAdapter<FixedExpenseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBudgetEntity = object : EntityInsertAdapter<BudgetEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `budgets` (`id`,`walletId`,`categoryName`,`limitAmount`,`period`,`currencyCode`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BudgetEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.walletId)
        statement.bindText(3, entity.categoryName)
        statement.bindDouble(4, entity.limitAmount)
        statement.bindText(5, entity.period)
        statement.bindText(6, entity.currencyCode)
      }
    }
    this.__insertAdapterOfSavingsGoalEntity = object : EntityInsertAdapter<SavingsGoalEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `savings_goals` (`id`,`name`,`targetAmount`,`currentAmount`,`deadline`,`currencyCode`,`color`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SavingsGoalEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.targetAmount)
        statement.bindDouble(4, entity.currentAmount)
        statement.bindLong(5, entity.deadline)
        statement.bindText(6, entity.currencyCode)
        statement.bindText(7, entity.color)
      }
    }
    this.__insertAdapterOfFixedExpenseEntity = object : EntityInsertAdapter<FixedExpenseEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `fixed_expenses` (`id`,`description`,`amount`,`categoryName`,`walletId`,`dayOfMonth`,`currencyCode`,`isActive`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FixedExpenseEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.description)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.categoryName)
        statement.bindText(5, entity.walletId)
        statement.bindLong(6, entity.dayOfMonth.toLong())
        statement.bindText(7, entity.currencyCode)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(8, _tmp.toLong())
      }
    }
  }

  public override suspend fun insertBudget(budget: BudgetEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfBudgetEntity.insert(_connection, budget)
  }

  public override suspend fun insertGoal(goal: SavingsGoalEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfSavingsGoalEntity.insert(_connection, goal)
  }

  public override suspend fun insertFixed(expense: FixedExpenseEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFixedExpenseEntity.insert(_connection, expense)
  }

  public override suspend fun getAllBudgets(): List<BudgetEntity> {
    val _sql: String = "SELECT * FROM budgets"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _cursorIndexOfCategoryName: Int = getColumnIndexOrThrow(_stmt, "categoryName")
        val _cursorIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _cursorIndexOfPeriod: Int = getColumnIndexOrThrow(_stmt, "period")
        val _cursorIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currencyCode")
        val _result: MutableList<BudgetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BudgetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpWalletId: String
          _tmpWalletId = _stmt.getText(_cursorIndexOfWalletId)
          val _tmpCategoryName: String
          _tmpCategoryName = _stmt.getText(_cursorIndexOfCategoryName)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_cursorIndexOfLimitAmount)
          val _tmpPeriod: String
          _tmpPeriod = _stmt.getText(_cursorIndexOfPeriod)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_cursorIndexOfCurrencyCode)
          _item =
              BudgetEntity(_tmpId,_tmpWalletId,_tmpCategoryName,_tmpLimitAmount,_tmpPeriod,_tmpCurrencyCode)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllGoals(): List<SavingsGoalEntity> {
    val _sql: String = "SELECT * FROM savings_goals"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _cursorIndexOfTargetAmount: Int = getColumnIndexOrThrow(_stmt, "targetAmount")
        val _cursorIndexOfCurrentAmount: Int = getColumnIndexOrThrow(_stmt, "currentAmount")
        val _cursorIndexOfDeadline: Int = getColumnIndexOrThrow(_stmt, "deadline")
        val _cursorIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currencyCode")
        val _cursorIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _result: MutableList<SavingsGoalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SavingsGoalEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_cursorIndexOfName)
          val _tmpTargetAmount: Double
          _tmpTargetAmount = _stmt.getDouble(_cursorIndexOfTargetAmount)
          val _tmpCurrentAmount: Double
          _tmpCurrentAmount = _stmt.getDouble(_cursorIndexOfCurrentAmount)
          val _tmpDeadline: Long
          _tmpDeadline = _stmt.getLong(_cursorIndexOfDeadline)
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_cursorIndexOfCurrencyCode)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_cursorIndexOfColor)
          _item =
              SavingsGoalEntity(_tmpId,_tmpName,_tmpTargetAmount,_tmpCurrentAmount,_tmpDeadline,_tmpCurrencyCode,_tmpColor)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllFixed(): List<FixedExpenseEntity> {
    val _sql: String = "SELECT * FROM fixed_expenses"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _cursorIndexOfCategoryName: Int = getColumnIndexOrThrow(_stmt, "categoryName")
        val _cursorIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _cursorIndexOfDayOfMonth: Int = getColumnIndexOrThrow(_stmt, "dayOfMonth")
        val _cursorIndexOfCurrencyCode: Int = getColumnIndexOrThrow(_stmt, "currencyCode")
        val _cursorIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _result: MutableList<FixedExpenseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FixedExpenseEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_cursorIndexOfAmount)
          val _tmpCategoryName: String
          _tmpCategoryName = _stmt.getText(_cursorIndexOfCategoryName)
          val _tmpWalletId: String
          _tmpWalletId = _stmt.getText(_cursorIndexOfWalletId)
          val _tmpDayOfMonth: Int
          _tmpDayOfMonth = _stmt.getLong(_cursorIndexOfDayOfMonth).toInt()
          val _tmpCurrencyCode: String
          _tmpCurrencyCode = _stmt.getText(_cursorIndexOfCurrencyCode)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          _item =
              FixedExpenseEntity(_tmpId,_tmpDescription,_tmpAmount,_tmpCategoryName,_tmpWalletId,_tmpDayOfMonth,_tmpCurrencyCode,_tmpIsActive)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBudgetById(id: String) {
    val _sql: String = "DELETE FROM budgets WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGoalById(id: String) {
    val _sql: String = "DELETE FROM savings_goals WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFixedById(id: String) {
    val _sql: String = "DELETE FROM fixed_expenses WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setFixedActive(id: String, isActive: Boolean) {
    val _sql: String = "UPDATE fixed_expenses SET isActive = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isActive) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
