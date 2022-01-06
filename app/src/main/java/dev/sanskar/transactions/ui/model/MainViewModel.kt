package dev.sanskar.transactions.ui.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import dev.sanskar.transactions.asFormattedDateTime
import dev.sanskar.transactions.data.DBInstanceHolder
import dev.sanskar.transactions.data.Transaction
import dev.sanskar.transactions.data.TransactionDatabase
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var db = Room.databaseBuilder(
        application,
        TransactionDatabase::class.java,
        "transactions"
    ).fallbackToDestructiveMigration()
        .allowMainThreadQueries()
        .build()

    init {
        DBInstanceHolder.db = this.db
    }

    val transactions = db.transactionDao().getAllTransactions()

    fun addTransaction(
        amount: Int,
        description: String,
        timestamp: Long,
        isDigital: Boolean = true,
        isExpense: Boolean = true,
    ) {
        val transaction = Transaction(
            0,
            amount,
            timestamp,
            isExpense,
            description,
            isDigital
        )

        viewModelScope.launch {
            db.transactionDao().insertTransaction(transaction)
        }
        Log.d(TAG, "addTransaction: added $transaction")
    }

    fun updateTransaction(
        id: Int,
        amount: Int,
        description: String,
        isDigital: Boolean = true,
        isExpense: Boolean = true,
        timestamp: Long
    ) {
        val transaction = Transaction(
            id,
            amount,
            timestamp,
            isExpense,
            description,
            isDigital
        )

        viewModelScope.launch {
            db.transactionDao().updateTransaction(transaction)
        }
        Log.d(TAG, "updateTransaction: updated $transaction")
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            db.transactionDao().deleteTransaction(transaction)
        }
        Log.d(TAG, "deleteTransaction: deleted $transaction")
    }

    fun clearTransactions() {
        viewModelScope.launch {
            db.transactionDao().clearTransactions()
        }

        Log.d(TAG, "clearTransactions: all transactions cleared!")
    }

    fun getDigitalBalance() = db.transactionDao().getTotalDigitalIncome() - db.transactionDao().getTotalDigitalExpenses()

    fun getCashBalance() = db.transactionDao().getTotalCashIncome() - db.transactionDao().getTotalCashExpenses()

    fun getTotalExpenses() = db.transactionDao().getTotalExpenses().toFloat()

    fun getCashExpense() = db.transactionDao().getTotalCashExpenses().toFloat()

    fun getDigitalExpense() = db.transactionDao().getTotalDigitalExpenses()

    fun getThisWeekExpense(): Int {
        Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            this.set(Calendar.HOUR_OF_DAY, 0)
            this.set(Calendar.MINUTE, 0)
            Log.d(TAG, "getThisWeekExpense: ${this.timeInMillis.asFormattedDateTime()}")
            return db.transactionDao().getThisWeekExpense(this.timeInMillis)
        }
    }
}