package com.upn3.proyecto_finanzas_personales.data

import androidx.room.*
import com.upn3.proyecto_finanzas_personales.model.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs WHERE transactionId = :txId ORDER BY timestamp ASC")
    fun getByTransaction(txId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs WHERE transactionId = :txId")
    suspend fun deleteByTransaction(txId: String)

    @Query("DELETE FROM audit_logs WHERE userEmail = :email")
    suspend fun deleteAllByUser(email: String)
}
