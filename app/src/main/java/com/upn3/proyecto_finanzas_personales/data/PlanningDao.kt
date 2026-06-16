package com.upn3.proyecto_finanzas_personales.data

import androidx.room.*
import com.upn3.proyecto_finanzas_personales.model.BudgetEntity
import com.upn3.proyecto_finanzas_personales.model.FixedExpenseEntity
import com.upn3.proyecto_finanzas_personales.model.SavingsGoalEntity

@Dao
interface PlanningDao {
    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String)

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoals(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT * FROM fixed_expenses")
    suspend fun getAllFixed(): List<FixedExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixed(expense: FixedExpenseEntity)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteFixedById(id: String)

    @Query("UPDATE fixed_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun setFixedActive(id: String, isActive: Boolean)
}
