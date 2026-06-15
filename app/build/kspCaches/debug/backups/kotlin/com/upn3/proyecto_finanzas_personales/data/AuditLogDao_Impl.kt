package com.upn3.proyecto_finanzas_personales.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.upn3.proyecto_finanzas_personales.model.AuditLogEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AuditLogDao_Impl(
  __db: RoomDatabase,
) : AuditLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAuditLogEntity: EntityInsertAdapter<AuditLogEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAuditLogEntity = object : EntityInsertAdapter<AuditLogEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `audit_logs` (`id`,`transactionId`,`action`,`timestamp`,`userEmail`,`changedFieldsJson`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AuditLogEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.transactionId)
        statement.bindText(3, entity.action)
        statement.bindLong(4, entity.timestamp)
        statement.bindText(5, entity.userEmail)
        statement.bindText(6, entity.changedFieldsJson)
      }
    }
  }

  public override suspend fun insert(log: AuditLogEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfAuditLogEntity.insert(_connection, log)
  }

  public override suspend fun insertAll(logs: List<AuditLogEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfAuditLogEntity.insert(_connection, logs)
  }

  public override fun getByTransaction(txId: String): Flow<List<AuditLogEntity>> {
    val _sql: String = "SELECT * FROM audit_logs WHERE transactionId = ? ORDER BY timestamp ASC"
    return createFlow(__db, false, arrayOf("audit_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, txId)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTransactionId: Int = getColumnIndexOrThrow(_stmt, "transactionId")
        val _cursorIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _cursorIndexOfUserEmail: Int = getColumnIndexOrThrow(_stmt, "userEmail")
        val _cursorIndexOfChangedFieldsJson: Int = getColumnIndexOrThrow(_stmt, "changedFieldsJson")
        val _result: MutableList<AuditLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AuditLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpTransactionId: String
          _tmpTransactionId = _stmt.getText(_cursorIndexOfTransactionId)
          val _tmpAction: String
          _tmpAction = _stmt.getText(_cursorIndexOfAction)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_cursorIndexOfTimestamp)
          val _tmpUserEmail: String
          _tmpUserEmail = _stmt.getText(_cursorIndexOfUserEmail)
          val _tmpChangedFieldsJson: String
          _tmpChangedFieldsJson = _stmt.getText(_cursorIndexOfChangedFieldsJson)
          _item =
              AuditLogEntity(_tmpId,_tmpTransactionId,_tmpAction,_tmpTimestamp,_tmpUserEmail,_tmpChangedFieldsJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByTransaction(txId: String) {
    val _sql: String = "DELETE FROM audit_logs WHERE transactionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, txId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllByUser(email: String) {
    val _sql: String = "DELETE FROM audit_logs WHERE userEmail = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, email)
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
