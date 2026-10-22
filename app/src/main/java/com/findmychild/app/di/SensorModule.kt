package com.findmychild.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * SensorModule — 空模块。
 *
 * LocationProvider 和 HeadingProvider 均标注了 @Singleton @Inject constructor，
 * Hilt 可自动发现并注入，无需手动 @Provides。
 * 保留此 Module 便于未来添加需要 @Provides 的传感器相关依赖。
 */
@Module
@InstallIn(SingletonComponent::class)
object SensorModule
