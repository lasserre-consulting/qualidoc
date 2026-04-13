package com.qualidoc

import com.qualidoc.domain.repository.RefreshTokenRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableScheduling
class QualiDocApplication

fun main(args: Array<String>) {
    runApplication<QualiDocApplication>(*args)
}

@Component
class RefreshTokenCleanupJob(private val refreshTokenRepository: RefreshTokenRepository) {

    // Tous les jours à 3h du matin
    @Scheduled(cron = "0 0 3 * * *")
    fun deleteExpiredTokens() {
        refreshTokenRepository.deleteExpired()
    }
}
