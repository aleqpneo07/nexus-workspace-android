package dev.quintero.nexus

import android.app.Application
import dev.quintero.nexus.feature.schedule.UniversityScheduleRepository

class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UniversityScheduleRepository.initialize(this)
    }
}
