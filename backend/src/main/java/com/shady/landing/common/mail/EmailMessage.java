package com.shady.landing.common.mail;

public record EmailMessage(String to, String subject, String textBody) {
}
