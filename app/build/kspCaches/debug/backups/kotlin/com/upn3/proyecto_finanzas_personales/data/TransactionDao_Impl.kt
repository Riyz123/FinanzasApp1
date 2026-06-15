package com.upn3.proyecto_finanzas_personales.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.upn3.proyecto_finanzas_personales.model.TransactionEntity
import javax.`annotation`.processing.Generated
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TransactionDao_Impl(
  __db: RoomDatabase,
) : TransactionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransactionEntity: EntityInsertAdapter<TransactionEntity>

  private val __deleteAdapterOfTransactionEntity: EntityDeleteOrUpdateAdapter<TransactionEntity>

  private val __updateAdapterOfTransactionEntity: EntityDeleteOrUpdateAdapter<TransactionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTransactionEntity = object : EntityInsertAdapter<TransactionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `transactions` (`id`,`userEmail`,`amount`,`description`,`origin`,`type`,`timestamp`,`latitude`,`longitude`,`lastModified`,`receiptPath`,`recipientName`,`recipientAlias`,`recipientBank`,`transferMotivo`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.userEmail)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.description)
        statement.bindText(5, entity.origin)
        statement.bindText(6, entity.type)
        statement.bindLong(7, entity.timestamp)
        val _tmpLatitude: Double? = entity.latitude
        if (_tmpLatitude == null) {
          statement.bindNull(8)
        } else {
          statement.bindDouble(8, _tmpLatitude)
        }
        val _tmpLongitude: Double? = entity.longitude
        if (_tmpLongitude == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpLongitude)
        }
        val _tmpLastModified: Long? = entity.lastModified
        if (_tmpLastModified == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpLastModified)
        }
        val _tmpReceiptPath: String? = entity.receiptPath
        if (_tmpReceiptPath == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpReceiptPath)
        }
        val _tmpRecipientName: String? = entity.recipientName
        if (_tmpRecipientName == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpRecipientName)
        }
        val _tmpRecipientAlias: String? = entity.recipientAlias
        if (_tmpRecipientAlias == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpRecipientAlias)
        }
        val _tmpRecipientBank: String? = entity.recipientBank
        if (_tmpRecipientBank == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpRecipientBank)
        }
        val _tmpTransferMotivo: String? = entity.transferMotivo
        if (_tmpTransferMotivo == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpTransferMotivo)
        }
      }
    }
    this.__deleteAdapterOfTransactionEntity = object :
        EntityDeleteOrUpdateAdapter<TransactionEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `transactions` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionEntity) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfTransactionEntity = object :
        EntityDeleteOrUpdateAdapter<TransactionEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `transactions` SET `id` = ?,`userEmail` = ?,`amount` = ?,`description` = ?,`origin` = ?,`type` = ?,`timestamp` = ?,`latitude` = ?,`longitude` = ?,`lastModified` = ?,`receiptPath` = ?,`recipientName` = ?,`recipientAlias` = ?,`recipientBank` = ?,`transferMotivo` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TransactionEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.userEmail)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.description)
        statement.bindText(5, entity.origin)
        statement.bindText(6, entity.type)
        statement.bindLong(7, entity.timestamp)
        val _tmpLatitude: Double? = entity.latitude
        if (_tmpLatitude == null) {
          statement.bindNull(8)
        } else {
          statement.bindDouble(8, _tmpLatitude)
        }
        val _tmpLongitude: Double? = entity.longitude
        if (_tmpLongitude == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpLongitude)
        }
        val _tmpLastModified: Long? = entity.lastModified
        if (_tmpLastModified == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpLastModified)
        }
        val _tmpReceiptPath: String? = entity.receiptPath
        if (_tmpReceiptPath == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpReceiptPath)
        }
        val _tmpRecipientName: String? = entity.recipientName
        if (_tmpRecipientName == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpRecipientName)
        }
        val _tmpRecipientAlias: String? = entity.recipientAlias
        if (_tmpRecipientAlias == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpRecipientAlias)
        }
        val _tmpRecipientBank: String? = entity.recipientBank
        if (_tmpRecipientBank == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpRecipientBank)
        }
        val _tmpTransferMotivo: String? = entity.transferMotivo
        if (_tmpTransferMotivo == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpTransferMotivo)
        }
        statement.bindText(16, entity.id)
      }
    }
  }

  public override suspend fun insert(transaction: TransactionEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfTransactionEntity.insert(_connection, transaction)
  }

  public override suspend fun insertAll(transactions: List<TransactionEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransactionEntity.insert(_connection, transactions)
  }

  public override suspend fun delete(transaction: TransactionEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfTransactionEntity.handle(_connection, transaction)
  }

  public override suspend fun update(transaction: TransactionEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfTransactionEntity.handle(_connection, transaction)
  }

  public override fun getTransactionsByUser(email: String): Flow<List<TransactionEntity>> {
    val _sql: String = "SELECT * FROM transactions WHERE userEmail = ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, email)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfUserEmail: Int = getColumnIndexOrThrow(_stmt, "userEmail")
        val _cursorIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _cursorIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _cursorIndexOfLastModified: Int = getColumnIndexOrThrow(_stmt, "lastModified")
        val _cursorIndexOfReceiptPath: Int = getColumnIndexOrThrow(_stmt, "receiptPath")
        val _cursorIndexOfRecipientName: Int = getColumnIndexOrThrow(_stmt, "recipientName")
        val _cursorIndexOfRecipientAlias: Int = getColumnIndexOrThrow(_stmt, "recipientAlias")
        val _cursorIndexOfRecipientBank: Int = getColumnIndexOrThrow(_stmt, "recipientBank")
        val _cursorIndexOfTransferMotivo: Int = getColumnIndexOrThrow(_stmt, "transferMotivo")
        val _result: MutableList<TransactionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TransactionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpUserEmail: String
          _tmpUserEmail = _stmt.getText(_cursorIndexOfUserEmail)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_cursorIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_cursorIndexOfOrigin)
          val _tmpType: String
          _tmpType = _stmt.getText(_cursorIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_cursorIndexOfTimestamp)
          val _tmpLatitude: Double?
          if (_stmt.isNull(_cursorIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_cursorIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_cursorIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_cursorIndexOfLongitude)
          }
          val _tmpLastModified: Long?
          if (_stmt.isNull(_cursorIndexOfLastModified)) {
            _tmpLastModified = null
          } else {
            _tmpLastModified = _stmt.getLong(_cursorIndexOfLastModified)
          }
          val _tmpReceiptPath: String?
          if (_stmt.isNull(_cursorIndexOfReceiptPath)) {
            _tmpReceiptPath = null
          } else {
            _tmpReceiptPath = _stmt.getText(_cursorIndexOfReceiptPath)
          }
          val _tmpRecipientName: String?
          if (_stmt.isNull(_cursorIndexOfRecipientName)) {
            _tmpRecipientName = null
          } else {
            _tmpRecipientName = _stmt.getText(_cursorIndexOfRecipientName)
          }
          val _tmpRecipientAlias: String?
          if (_stmt.isNull(_cursorIndexOfRecipientAlias)) {
            _tmpRecipientAlias = null
          } else {
            _tmpRecipientAlias = _stmt.getText(_cursorIndexOfRecipientAlias)
          }
          val _tmpRecipientBank: String?
          if (_stmt.isNull(_cursorIndexOfRecipientBank)) {
            _tmpRecipientBank = null
          } else {
            _tmpRecipientBank = _stmt.getText(_cursorIndexOfRecipientBank)
          }
          val _tmpTransferMotivo: String?
          if (_stmt.isNull(_cursorIndexOfTransferMotivo)) {
            _tmpTransferMotivo = null
          } else {
            _tmpTransferMotivo = _stmt.getText(_cursorIndexOfTransferMotivo)
          }
          _item =
              TransactionEntity(_tmpId,_tmpUserEmail,_tmpAmount,_tmpDescription,_tmpOrigin,_tmpType,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpLastModified,_tmpReceiptPath,_tmpRecipientName,_tmpRecipientAlias,_tmpRecipientBank,_tmpTransferMotivo)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): TransactionEntity? {
    val _sql: String = "SELECT * FROM transactions WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfUserEmail: Int = getColumnIndexOrThrow(_stmt, "userEmail")
        val _cursorIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _cursorIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _cursorIndexOfLastModified: Int = getColumnIndexOrThrow(_stmt, "lastModified")
        val _cursorIndexOfReceiptPath: Int = getColumnIndexOrThrow(_stmt, "receiptPath")
        val _cursorIndexOfRecipientName: Int = getColumnIndexOrThrow(_stmt, "recipientName")
        val _cursorIndexOfRecipientAlias: Int = getColumnIndexOrThrow(_stmt, "recipientAlias")
        val _cursorIndexOfRecipientBank: Int = getColumnIndexOrThrow(_stmt, "recipientBank")
        val _cursorIndexOfTransferMotivo: Int = getColumnIndexOrThrow(_stmt, "transferMotivo")
        val _result: TransactionEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpUserEmail: String
          _tmpUserEmail = _stmt.getText(_cursorIndexOfUserEmail)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_cursorIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_cursorIndexOfOrigin)
          val _tmpType: String
          _tmpType = _stmt.getText(_cursorIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_cursorIndexOfTimestamp)
          val _tmpLatitude: Double?
          if (_stmt.isNull(_cursorIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_cursorIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_cursorIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_cursorIndexOfLongitude)
          }
          val _tmpLastModified: Long?
          if (_stmt.isNull(_cursorIndexOfLastModified)) {
            _tmpLastModified = null
          } else {
            _tmpLastModified = _stmt.getLong(_cursorIndexOfLastModified)
          }
          val _tmpReceiptPath: String?
          if (_stmt.isNull(_cursorIndexOfReceiptPath)) {
            _tmpReceiptPath = null
          } else {
            _tmpReceiptPath = _stmt.getText(_cursorIndexOfReceiptPath)
          }
          val _tmpRecipientName: String?
          if (_stmt.isNull(_cursorIndexOfRecipientName)) {
            _tmpRecipientName = null
          } else {
            _tmpRecipientName = _stmt.getText(_cursorIndexOfRecipientName)
          }
          val _tmpRecipientAlias: String?
          if (_stmt.isNull(_cursorIndexOfRecipientAlias)) {
            _tmpRecipientAlias = null
          } else {
            _tmpRecipientAlias = _stmt.getText(_cursorIndexOfRecipientAlias)
          }
          val _tmpRecipientBank: String?
          if (_stmt.isNull(_cursorIndexOfRecipientBank)) {
            _tmpRecipientBank = null
          } else {
            _tmpRecipientBank = _stmt.getText(_cursorIndexOfRecipientBank)
          }
          val _tmpTransferMotivo: String?
          if (_stmt.isNull(_cursorIndexOfTransferMotivo)) {
            _tmpTransferMotivo = null
          } else {
            _tmpTransferMotivo = _stmt.getText(_cursorIndexOfTransferMotivo)
          }
          _result =
              TransactionEntity(_tmpId,_tmpUserEmail,_tmpAmount,_tmpDescription,_tmpOrigin,_tmpType,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpLastModified,_tmpReceiptPath,_tmpRecipientName,_tmpRecipientAlias,_tmpRecipientBank,_tmpTransferMotivo)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM transactions WHERE id = ?"
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

  public override suspend fun deleteAllByUser(email: String) {
    val _sql: String = "DELETE FROM transactions WHERE userEmail = ?"
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
