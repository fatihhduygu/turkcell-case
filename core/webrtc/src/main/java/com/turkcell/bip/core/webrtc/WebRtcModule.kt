package com.turkcell.bip.core.webrtc

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebRtcModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideBipRtcClientFactory(
        @ApplicationContext context: Context
    ): BipRtcClientFactory = BipRtcClientFactory { observer, onSend ->
        BipRtcClient(context.applicationContext as Application, observer, onSend)
    }
}
