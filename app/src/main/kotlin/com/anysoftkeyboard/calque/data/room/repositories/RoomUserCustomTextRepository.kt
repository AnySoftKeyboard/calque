package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.data.room.dao.UserCustomTextDao
import com.anysoftkeyboard.calque.data.room.entities.UserCustomTextEntity
import com.anysoftkeyboard.calque.data.user.UserCustomTextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [UserCustomTextRepository] using [UserCustomTextDao]. */
class RoomUserCustomTextRepository(
    val dao: UserCustomTextDao,
) : UserCustomTextRepository {

    override fun insert(text: UserCustomText): Long {
        return runBlocking(Dispatchers.IO) { dao.insert(UserCustomTextEntity.fromDomain(text)) }
    }

    override fun update(text: UserCustomText): Boolean {
        if (text.textId <= 0L) return false
        return runBlocking(Dispatchers.IO) { dao.update(UserCustomTextEntity.fromDomain(text)) > 0 }
    }

    override fun getById(textId: Long): UserCustomText? {
        if (textId <= 0L) return null
        return runBlocking(Dispatchers.IO) { dao.getById(textId)?.toDomain() }
    }

    override fun getAll(): List<UserCustomText> {
        return runBlocking(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
    }

    override fun delete(textId: Long): Boolean {
        if (textId <= 0L) return false
        return runBlocking(Dispatchers.IO) { dao.delete(textId) > 0 }
    }
}
