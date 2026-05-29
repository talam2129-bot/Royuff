package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class WalletRepository(private val walletDao: WalletDao) {

    val userProfileFlow: Flow<UserProfile?> = walletDao.getUserProfileFlow()
    val allTransactionsFlow: Flow<List<TransactionHistory>> = walletDao.getAllTransactionsFlow()
    val savedBillersFlow: Flow<List<SavedBiller>> = walletDao.getSavedBillersFlow()
    val allBudgetsFlow: Flow<List<CategoryBudget>> = walletDao.getAllBudgetsFlow()

    suspend fun seedInitialDataIfNecessary() {
        val currentUser = walletDao.getUserProfile()
        if (currentUser == null) {
            // Seed User Profile
            val initialUserProfile = UserProfile(
                id = 1,
                name = "Sandesh Adhikari",
                phoneNumber = "+977 9851234567",
                balanceNpr = 24550.0,
                kycVerified = true,
                isPinSet = true,
                securePin = "1234",
                accountNumber = "NP-NEOPAY-382910"
            )
            walletDao.insertUserProfile(initialUserProfile)

            // Seed Initial Transactions classified with Categories for Budgeting
            val initialTxs = listOf(
                TransactionHistory(
                    title = "Received via NeoPay",
                    subtitle = "From Prasanna Thapa (Transfer)",
                    amountNpr = 3500.0,
                    type = "RECEIVE",
                    cashbackAmount = 0.0,
                    category = "Other",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2 // 2 hours ago
                ),
                TransactionHistory(
                    title = "NEA Utility Bill",
                    subtitle = "Kathmandu Counter (Consumer: 20491823)",
                    amountNpr = 1200.0,
                    type = "BILL_UTILITY",
                    cashbackAmount = 12.0,
                    category = "Utilities",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 // 1 day ago
                ),
                TransactionHistory(
                    title = "Bhatbhateni Supermarket",
                    subtitle = "Weekly grocery & snacks shopping",
                    amountNpr = 4320.0,
                    type = "SEND",
                    cashbackAmount = 0.0,
                    category = "Food",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 30 // 1.25 days ago
                ),
                TransactionHistory(
                    title = "Worldlink Internet",
                    subtitle = "Account: WL-NEO-8839 (Yearly Package)",
                    amountNpr = 1850.0,
                    type = "BILL_UTILITY",
                    cashbackAmount = 37.0,
                    category = "Utilities",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48 // 2 days ago
                ),
                TransactionHistory(
                    title = "Pathao Ride Sharing",
                    subtitle = "Office commute (Kathmandu)",
                    amountNpr = 340.0,
                    type = "SEND",
                    cashbackAmount = 0.0,
                    category = "Transport",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 50 // 2 days ago
                ),
                TransactionHistory(
                    title = "Ncell Recharge",
                    subtitle = "Top-up to +977 9801234567",
                    amountNpr = 500.0,
                    type = "RECHARGE",
                    cashbackAmount = 15.0,
                    category = "Recharge",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 72 // 3 days ago
                ),
                TransactionHistory(
                    title = "QFX Cinemas Movie Ticket",
                    subtitle = "Weekend movie with friends",
                    amountNpr = 900.0,
                    type = "SEND",
                    cashbackAmount = 0.0,
                    category = "Entertainment",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 80 // ~3 days ago
                ),
                TransactionHistory(
                    title = "Bank Link Transfer",
                    subtitle = "Sent to NIC ASIA bank a/c",
                    amountNpr = 5000.0,
                    type = "SEND",
                    cashbackAmount = 0.0,
                    category = "Other",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 120 // 5 days ago
                )
            )
            for (tx in initialTxs) {
                walletDao.insertTransaction(tx)
            }

            // Seed Saved Billers
            val initialBillers = listOf(
                SavedBiller(
                    name = "Nepal Electricity Authority (NEA)",
                    utilityType = "ELECTRICITY",
                    accountNumber = "20491823",
                    details = "NEA - Kathmandu Central"
                ),
                SavedBiller(
                    name = "Worldlink Communication",
                    utilityType = "INTERNET",
                    accountNumber = "WL-NEO-8839",
                    details = "150 Mbps Fiber Home"
                ),
                SavedBiller(
                    name = "Khanepani Water",
                    utilityType = "WATER",
                    accountNumber = "9912048",
                    details = "KUKL Lalitpur Branch"
                )
            )
            for (biller in initialBillers) {
                walletDao.insertSavedBiller(biller)
            }

            // Seed Default Budgets
            val defaultBudgets = listOf(
                CategoryBudget(category = "Food", limitNpr = 8000.0),
                CategoryBudget(category = "Transport", limitNpr = 2500.0),
                CategoryBudget(category = "Entertainment", limitNpr = 3000.0),
                CategoryBudget(category = "Utilities", limitNpr = 6000.0),
                CategoryBudget(category = "Recharge", limitNpr = 1000.0)
            )
            for (budget in defaultBudgets) {
                walletDao.insertBudget(budget)
            }
        }
    }

    // 1. Send Money (Wallet to Wallet) with optional category
    suspend fun sendMoney(recipientPhone: String, amount: Double, note: String, category: String = "Other"): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        if (user.balanceNpr < amount) return false

        // Deduct balance
        val updatedUser = user.copy(balanceNpr = user.balanceNpr - amount)
        walletDao.updateUserProfile(updatedUser)

        // Record transaction
        walletDao.insertTransaction(
            TransactionHistory(
                title = "Sent to +977 $recipientPhone",
                subtitle = "NeoPay Transfer: $note",
                amountNpr = amount,
                type = "SEND",
                cashbackAmount = 0.0,
                category = category
            )
        )
        return true
    }

    // 2. Bank Transfer with optional category
    suspend fun bankTransfer(bankName: String, accountNum: String, accountName: String, amount: Double, note: String, category: String = "Other"): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        if (user.balanceNpr < amount) return false

        val updatedUser = user.copy(balanceNpr = user.balanceNpr - amount)
        walletDao.updateUserProfile(updatedUser)

        walletDao.insertTransaction(
            TransactionHistory(
                title = "Bank Transfer ($bankName)",
                subtitle = "To $accountName (A/C: $accountNum)",
                amountNpr = amount,
                type = "SEND",
                cashbackAmount = 0.0,
                category = category
            )
        )
        return true
    }

    // 3. Mobile Recharge & Top-up
    suspend fun mobileRecharge(phone: String, operator: String, amount: Double, category: String = "Recharge"): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        if (user.balanceNpr < amount) return false

        // 3% cashback on mobile topup
        val cashback = amount * 0.03
        val updatedUser = user.copy(balanceNpr = user.balanceNpr - amount + cashback)
        walletDao.updateUserProfile(updatedUser)

        walletDao.insertTransaction(
            TransactionHistory(
                title = "$operator Top-up",
                subtitle = "Recharge to +977 $phone",
                amountNpr = amount,
                type = "RECHARGE",
                cashbackAmount = cashback,
                category = category
            )
        )
        return true
    }

    // 4. Utility Bill Payment
    suspend fun payUtilityBill(billerName: String, utilityType: String, accountNo: String, amount: Double, category: String = "Utilities"): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        if (user.balanceNpr < amount) return false

        // 1.5% cashback on general internet or utilities
        val cashbackRate = if (utilityType == "INTERNET") 0.02 else 0.01
        val cashback = amount * cashbackRate
        val updatedUser = user.copy(balanceNpr = user.balanceNpr - amount + cashback)
        walletDao.updateUserProfile(updatedUser)

        walletDao.insertTransaction(
            TransactionHistory(
                title = billerName,
                subtitle = "A/C: $accountNo Payment",
                amountNpr = amount,
                type = "BILL_UTILITY",
                cashbackAmount = cashback,
                category = category
            )
        )
        return true
    }

    // 5. Simulated Add Funds (e.g. from Linked Bank or SIM Card mock)
    suspend fun addFunds(source: String, amount: Double): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        val updatedUser = user.copy(balanceNpr = user.balanceNpr + amount)
        walletDao.updateUserProfile(updatedUser)

        walletDao.insertTransaction(
            TransactionHistory(
                title = "Added Funds",
                subtitle = "Loaded via $source",
                amountNpr = amount,
                type = "RECEIVE",
                cashbackAmount = 0.0,
                category = "Other"
            )
        )
        return true
    }

    // Save standard utility biller
    suspend fun saveBiller(name: String, utilityType: String, accountNum: String, details: String) {
        walletDao.insertSavedBiller(
            SavedBiller(
                name = name,
                utilityType = utilityType,
                accountNumber = accountNum,
                details = details
            )
        )
    }

    suspend fun deleteBiller(id: Int) {
        walletDao.deleteSavedBiller(id)
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        return user.securePin == enteredPin
    }

    // Budget modifiers
    suspend fun setBudgetLimit(category: String, limit: Double, isEnabled: Boolean = true) {
        walletDao.insertBudget(CategoryBudget(category, limit, isEnabled))
    }

    suspend fun deleteBudget(category: String) {
        walletDao.deleteBudget(category)
    }

    suspend fun updateFingerprintEnabled(enabled: Boolean): Boolean {
        val user = walletDao.getUserProfile() ?: return false
        val updatedUser = user.copy(isFingerprintEnabled = enabled)
        walletDao.updateUserProfile(updatedUser)
        return true
    }
}
