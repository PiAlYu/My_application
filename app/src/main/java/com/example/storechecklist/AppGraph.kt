package com.example.storechecklist

import android.content.Context
import androidx.room.Room
import com.example.storechecklist.data.local.AppDatabase
import com.example.storechecklist.data.local.ServerConfigStore
import com.example.storechecklist.data.remote.ServerApiFactory
import com.example.storechecklist.data.repository.ChecklistRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object AppGraph {
    private var isInitialized = false

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ChecklistRepository
        private set

    fun init(context: Context) {
        if (isInitialized) return

        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "store_checklist.db",
        ).build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val converterFactory = MoshiConverterFactory.create(moshi)
        val serverApiFactory = ServerApiFactory(converterFactory)
        val serverConfigStore = ServerConfigStore(context.applicationContext)
        repository = ChecklistRepository(
            database = database,
            checklistDao = database.checklistDao(),
            serverApiFactory = serverApiFactory,
            serverConfigStore = serverConfigStore,
        )

        isInitialized = true
    }
}
