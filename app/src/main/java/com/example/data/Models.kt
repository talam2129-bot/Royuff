package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only 1 local user
    val name: String,
    val phoneNumber: String,
    val balanceNpr: Double,
    val kycVerified: Boolean = true,
    val isPinSet: Boolean = true,
    val securePin: String = "1234", // Simple default PIN
    val accountNumber: String = "NP-NEOPAY-382910",
    val isFingerprintEnabled: Boolean = true
)

@Entity(tableName = "transaction_history")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subtitle: String,
    val amountNpr: Double,
    val type: String, // "SEND", "RECEIVE", "RECHARGE", "BILL_UTILITY", "CASHBACK"
    val timestamp: Long = System.currentTimeMillis(),
    val transactionCode: String = "TXN" + UUID.randomUUID().toString().take(8).uppercase(),
    val cashbackAmount: Double = 0.0,
    val category: String = "Other"
)

@Entity(tableName = "saved_biller")
data class SavedBiller(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val utilityType: String, // "ELECTRICITY", "WATER", "INTERNET"
    val accountNumber: String,
    val details: String = ""
)

@Entity(tableName = "category_budget")
data class CategoryBudget(
    @PrimaryKey val category: String, // e.g. "Food", "Transport", "Entertainment", "Utilities", "Recharge", "Other"
    val limitNpr: Double,
    val isEnabled: Boolean = true
)
