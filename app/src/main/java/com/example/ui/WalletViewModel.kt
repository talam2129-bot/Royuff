package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedBiller
import com.example.data.TransactionHistory
import com.example.data.UserProfile
import com.example.data.WalletRepository
import com.example.data.CategoryBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface PaymentStatus {
    object Idle : PaymentStatus
    object EnteringDetails : PaymentStatus
    object AwaitingPin : PaymentStatus
    object AwaitingOtp : PaymentStatus { val otpCode = "1203" } // Fast simulated secure OTP code
    object Processing : PaymentStatus
    data class Success(val message: String, val cashbackEarned: Double = 0.0) : PaymentStatus
    data class Error(val message: String) : PaymentStatus
}

class WalletViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    // 1. Reactive Datastores
    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allTransactions: StateFlow<List<TransactionHistory>> = repository.allTransactionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedBillers: StateFlow<List<SavedBiller>> = repository.savedBillersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBudgets: StateFlow<List<CategoryBudget>> = repository.allBudgetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive alert evaluator combining transactions and budgets
    val budgetAlerts: StateFlow<List<String>> = combine(allTransactions, allBudgets) { txs, budgets ->
        val alerts = mutableListOf<String>()
        budgets.forEach { budget ->
            if (budget.isEnabled && budget.limitNpr > 0.0) {
                val spent = txs.filter {
                    it.category.equals(budget.category, ignoreCase = true) &&
                    it.type != "RECEIVE"
                }.sumOf { it.amountNpr }

                val limit = budget.limitNpr
                if (spent >= limit) {
                    alerts.add("⚠️ EXCEEDED: Your spending of Rs. ${String.format("%,.1f", spent)} in '${budget.category}' is over your Rs. ${String.format("%,.1f", limit)} limit!")
                } else if (spent >= limit * 0.8) {
                    alerts.add("🔔 APPROACHING LIMIT: Spending in '${budget.category}' has reached Rs. ${String.format("%,.1f", spent)} (${String.format("%.0f", (spent / limit) * 100)}% of Rs. ${String.format("%,.1f", limit)}).")
                }
            }
        }
        alerts
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Initialize dynamic Nepalese demo data if database is empty on launch
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
        }
    }

    // 2. Interactive Operational State (Single-View UI transitions)
    private val _currentTab = MutableStateFlow("HOME") // "HOME", "SCAN_PAY", "HISTORY", "BUDGET", "OFFERS"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateFingerprintEnabled(enabled)
        }
    }

    private val _paymentStatus = MutableStateFlow<PaymentStatus>(PaymentStatus.Idle)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
        if (tab != "SCAN_PAY") {
            resetPaymentStatus()
        }
    }

    fun resetPaymentStatus() {
        _paymentStatus.value = PaymentStatus.Idle
    }

    // --- FEATURE A: WALLET TO WALLET TRANSFER ---
    fun executeSendMoney(phone: String, amountStr: String, note: String, pin: String, category: String = "Other") {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _paymentStatus.value = PaymentStatus.Error("Please enter a valid amount in NPR")
            return
        }

        viewModelScope.launch {
            _paymentStatus.value = PaymentStatus.Processing
            
            // Check PIN security
            val isPinValid = repository.verifyPin(pin)
            if (!isPinValid) {
                _paymentStatus.value = PaymentStatus.Error("Incorrect NeoPay Security PIN")
                return@launch
            }

            val success = repository.sendMoney(phone, amount, note, category)
            if (success) {
                _paymentStatus.value = PaymentStatus.Success(
                    message = "Successfully transferred Rs. $amount to +977 $phone!"
                )
            } else {
                _paymentStatus.value = PaymentStatus.Error("Insufficient balance in your wallet")
            }
        }
    }

    // --- FEATURE B: BANK LINK TRANSFER ---
    fun executeBankTransfer(bankName: String, accountNum: String, accountName: String, amountStr: String, note: String, pin: String, category: String = "Other") {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _paymentStatus.value = PaymentStatus.Error("Please enter a valid transaction amount")
            return
        }
        if (accountNum.isBlank() || accountName.isBlank()) {
            _paymentStatus.value = PaymentStatus.Error("Please complete bank details correctly")
            return
        }

        viewModelScope.launch {
            _paymentStatus.value = PaymentStatus.Processing

            // Check PIN security
            val isPinValid = repository.verifyPin(pin)
            if (!isPinValid) {
                _paymentStatus.value = PaymentStatus.Error("Incorrect NeoPay Security PIN")
                return@launch
            }

            val success = repository.bankTransfer(bankName, accountNum, accountName, amount, note, category)
            if (success) {
                _paymentStatus.value = PaymentStatus.Success(
                    message = "Transferred Rs. $amount to $bankName Account: $accountNum"
                )
            } else {
                _paymentStatus.value = PaymentStatus.Error("Insufficient balance in your wallet")
            }
        }
    }

    // --- FEATURE C: MOBILE TOP-UP RECHARGE ---
    fun executeMobileRecharge(phone: String, opName: String, amountStr: String, pin: String, category: String = "Recharge") {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _paymentStatus.value = PaymentStatus.Error("Please enter a valid recharge amount")
            return
        }
        if (phone.length < 10) {
            _paymentStatus.value = PaymentStatus.Error("Ensure mobile number is a valid 10-digit Nepali format")
            return
        }

        viewModelScope.launch {
            _paymentStatus.value = PaymentStatus.Processing

            // Check PIN security
            val isPinValid = repository.verifyPin(pin)
            if (!isPinValid) {
                _paymentStatus.value = PaymentStatus.Error("Incorrect NeoPay Security PIN")
                return@launch
            }

            val cashbackCalculated = amount * 0.03
            val success = repository.mobileRecharge(phone, opName, amount, category)
            if (success) {
                _paymentStatus.value = PaymentStatus.Success(
                    message = "Recharge of Rs. $amount to +977 $phone completed successfully!",
                    cashbackEarned = cashbackCalculated
                )
            } else {
                _paymentStatus.value = PaymentStatus.Error("Insufficient wallet funds")
            }
        }
    }

    // --- FEATURE D: UTILITY BILLS PAYMENT ---
    fun executeUtilityBill(billerName: String, type: String, accountNum: String, amountStr: String, pin: String, category: String = "Utilities") {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _paymentStatus.value = PaymentStatus.Error("Please enter a valid utility fee amount")
            return
        }

        viewModelScope.launch {
            _paymentStatus.value = PaymentStatus.Processing

            // Check PIN security
            val isPinValid = repository.verifyPin(pin)
            if (!isPinValid) {
                _paymentStatus.value = PaymentStatus.Error("Incorrect NeoPay Security PIN")
                return@launch
            }

            val cashbackRate = if (type == "INTERNET") 0.02 else 0.01
            val cashbackCalculated = amount * cashbackRate
            val success = repository.payUtilityBill(billerName, type, accountNum, amount, category)
            if (success) {
                _paymentStatus.value = PaymentStatus.Success(
                    message = "Bill Payment of Rs. $amount to $billerName completed successfully!",
                    cashbackEarned = cashbackCalculated
                )
                // Add to saved billers if not already saved
                repository.saveBiller(billerName, type, accountNum, "$type - AutoSaved Account")
            } else {
                _paymentStatus.value = PaymentStatus.Error("Insufficient balance in NeoPay wallet")
            }
        }
    }

    // --- FEATURE E: SIMULATED LOAD MONEY (FUND RECHARGE) ---
    fun executeLoadMoney(source: String, amountStr: String) {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _paymentStatus.value = PaymentStatus.Error("Please enter a valid fund amount to load")
            return
        }

        viewModelScope.launch {
            _paymentStatus.value = PaymentStatus.Processing
            val success = repository.addFunds(source, amount)
            if (success) {
                _paymentStatus.value = PaymentStatus.Success(
                    message = "Successfully loaded Rs. $amount into your NeoPay wallet from $source!"
                )
            } else {
                _paymentStatus.value = PaymentStatus.Error("Unable to communicate with linked bank/source")
            }
        }
    }

    // --- BUDGET MODIFIERS ---
    fun setBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.setBudgetLimit(category, limit)
        }
    }

    fun deleteBudgetLimit(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    // --- FEATURE F: DYNAMIC OPERATOR IDENTIFIER ---
    fun resolveMobileOperator(phone: String): String {
        if (phone.length >= 3) {
            val prefix3 = phone.take(3)
            return when (prefix3) {
                "980", "981", "982", "970" -> "Ncell Axiata"
                "984", "985", "986", "974", "975", "976" -> "NTC Namaste"
                "961", "962", "988" -> "Smart Cell"
                else -> "Nepal Telecom"
            }
        }
        return "Nepal Telecom"
    }

    // Factory Provider
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = WalletRepository(database.walletDao())
                    return WalletViewModel(application, repository) as T
                }
            }
        }
    }
}
