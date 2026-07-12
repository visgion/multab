package com.example.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): ProfileEntity? {
        return profileDao.getProfileById(id)
    }

    suspend fun insert(profile: ProfileEntity): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun update(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    suspend fun delete(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }
}
