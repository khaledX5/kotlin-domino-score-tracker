package io.github.khaledx5.dominoscore.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.khaledx5.dominoscore.share.PngResultImageExporter
import io.github.khaledx5.dominoscore.share.ResultImageExporter

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {
    @Binds
    abstract fun bindResultImageExporter(exporter: PngResultImageExporter): ResultImageExporter
}
