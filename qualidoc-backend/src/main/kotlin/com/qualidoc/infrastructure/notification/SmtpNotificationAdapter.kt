package com.qualidoc.infrastructure.notification

import com.qualidoc.domain.port.EmailMessage
import com.qualidoc.domain.port.NotificationPort
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class SmtpNotificationAdapter(
    private val mailSender: JavaMailSender
) : NotificationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(message: EmailMessage) {
        try {
            val mime = mailSender.createMimeMessage()
            MimeMessageHelper(mime, true, "UTF-8").apply {
                setTo(message.to)
                setSubject(message.subject)
                setText(message.body, message.isHtml)
                setFrom("noreply@qualidoc.fr")
            }
            mailSender.send(mime)
            log.info("Email envoyé à ${message.to} — ${message.subject}")
        } catch (ex: Exception) {
            log.error("Échec envoi email à ${message.to} : ${ex.message}", ex)
            // Ne pas propager — l'échec d'une notif ne doit pas faire échouer la transaction
        }
    }

    override fun sendBulk(messages: List<EmailMessage>) {
        if (messages.isEmpty()) return
        log.info("Envoi groupé de ${messages.size} email(s)")
        messages.forEach { send(it) }
    }
}
