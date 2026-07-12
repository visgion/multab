package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM browser_profiles ORDER BY timestamp DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM browser_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)
}
