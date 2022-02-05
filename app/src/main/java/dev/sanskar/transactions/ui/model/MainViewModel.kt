package dev.sanskar.transactions.ui.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import dev.sanskar.transactions.asFormattedDateTime
import dev.sanskar.transactions.data.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    object QueryConfig {
        var filterAmountChoice = FilterByAmountChoices.UNSPECIFIED
        var filterAmountValue = -1
        var filterTypeChoice = FilterByTypeChoices.UNSPECIFIED
        var filterMediumChoice = FilterByMediumChoices.UNSPECIFIED
        var sortChoice = SortByChoices.UNSPECIFIED
    }

    private var db = Room.databaseBuilder(
        application,
        TransactionDatabase::class.java,
        "transactions"
    ).allowMainThreadQueries()
        .build()


    init {
        DBInstanceHolder.db = this.db
//        getAll()
        executeConfig()
    }

    val transactions = MutableLiveData<List<Transaction>>()

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

    fun setSortMethod(index: Int) {
        QueryConfig.sortChoice = SortByChoices.values().find {
            it.ordinal == index
        } ?: SortByChoices.UNSPECIFIED
        executeConfig()
    }

    fun setFilterType(index: Int) {
        QueryConfig.filterTypeChoice = FilterByTypeChoices.values().find {
            it.ordinal == index
        } ?: FilterByTypeChoices.UNSPECIFIED
        executeConfig()
    }

    fun setFilterMedium(index: Int) {
        QueryConfig.filterMediumChoice = FilterByMediumChoices.values().find {
            it.ordinal == index
        } ?: FilterByMediumChoices.UNSPECIFIED
        executeConfig()
    }

    fun setFilterAmount(amount: Int, index: Int) {
        QueryConfig.filterAmountChoice = FilterByAmountChoices.values().find {
            it.ordinal == index
        } ?: FilterByAmountChoices.UNSPECIFIED
        QueryConfig.filterAmountValue = amount
        executeConfig()
    }

    private fun executeConfig() {
        val query = QueryBuilder()
            .setFilterAmount(QueryConfig.filterAmountChoice, QueryConfig.filterAmountValue)
            .setFilterType(QueryConfig.filterTypeChoice)
            .setFilterMedium(QueryConfig.filterMediumChoice)
            .setSortingChoice(QueryConfig.sortChoice)
            .build()
        viewModelScope.launch {
            db.transactionDao().customTransactionQuery(query).collect {
                transactions.value = it
            }
        }
    }
}