package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // Transaction History queries
    @Query("SELECT * FROM transaction_history ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionHistory)

    // Saved Billers
    @Query("SELECT * FROM saved_biller ORDER BY name ASC")
    fun getSavedBillersFlow(): Flow<List<SavedBiller>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedBiller(biller: SavedBiller)

    @Query("DELETE FROM saved_biller WHERE id = :id")
    suspend fun deleteSavedBiller(id: Int)

    // Category Budgets
    @Query("SELECT * FROM category_budget ORDER BY category ASC")
    fun getAllBudgetsFlow(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budget WHERE category = :category")
    suspend fun deleteBudget(category: String)
}
