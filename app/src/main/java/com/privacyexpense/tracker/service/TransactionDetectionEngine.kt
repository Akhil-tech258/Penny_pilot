package com.privacyexpense.tracker.service

import com.privacyexpense.tracker.data.model.Transaction
import com.privacyexpense.tracker.data.model.TransactionStatus
import com.privacyexpense.tracker.data.model.TransactionType
import java.util.regex.Pattern

object TransactionDetectionEngine {

    data class DetectionResult(
        val isTransaction: Boolean,
        val transaction: Transaction? = null
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:(?:[₹]|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:rupees?|rupee|rs|inr))""",
        Pattern.CASE_INSENSITIVE
    )

    // Extensive keywords that identify marketing, investments, SIPs, loans, or non-transaction alerts
    private val IGNORE_KEYWORDS = listOf(
        "otp", "due date", "bill due", "reminder", "offer", "discount", "statement", "loan approved",
        "sip", "mutual fund", "fund", "invest", "portfolio", "stocks", "gold",
        "start a", "start your", "starting at", "starting from", "starting @", "starting ₹", "starting rs",
        "reward", "scratch card", "win ", "won ", "cashback of up to", "earn up to", "earn ₹", "earn rs",
        "pre-approved", "pre approved", "apply for", "apply now", "apply today", "kyc", "cibil", "credit score",
        "gift card", "spin the wheel", "spin & win", "voucher", "insurance", "save daily", "grow your wealth",
        "claim your", "congratulations", "bonus", "unlock", "explore"
    )

    fun parseNotification(
        packageName: String,
        title: String?,
        text: String?
    ): DetectionResult {
        val fullContent = "${title ?: ""} ${text ?: ""}".trim()
        if (fullContent.isBlank()) return DetectionResult(false)

        val lowerContent = fullContent.lowercase()

        // 1. Strict filter against promotional, SIP, mutual fund, or engagement notifications
        for (ignore in IGNORE_KEYWORDS) {
            if (lowerContent.contains(ignore)) {
                return DetectionResult(false)
            }
        }

        // 2. Determine Transaction Type (Context-Aware Credit vs Debit)
        val type = determineTransactionType(lowerContent) ?: return DetectionResult(false)

        // 3. Extract Amount
        val amountMatcher = AMOUNT_PATTERN.matcher(fullContent)
        val amount: Double
        if (amountMatcher.find()) {
            val amountStr = (amountMatcher.group(1) ?: amountMatcher.group(2))?.replace(",", "") ?: ""
            amount = amountStr.toDoubleOrNull() ?: return DetectionResult(false)
        } else {
            return DetectionResult(false)
        }

        // 4. Extract Merchant / Sender according to transaction direction
        val merchant = extractMerchant(title, text, fullContent, type)

        // 5. Resolve friendly source app name
        val sourceApp = resolveAppName(packageName)

        val transaction = Transaction(
            amount = amount,
            currency = "INR",
            type = type,
            merchant = merchant,
            description = "",
            sourceApp = sourceApp,
            sourcePackage = packageName,
            transactionTime = System.currentTimeMillis(),
            detectedTime = System.currentTimeMillis(),
            status = TransactionStatus.PENDING
        )

        return DetectionResult(isTransaction = true, transaction = transaction)
    }

    private fun determineTransactionType(lowerContent: String): TransactionType? {
        // High-confidence CREDIT patterns:
        // "Abhi sent you...", "sent you ₹...", "credited to...", "received from...", "deposited into...", "cashback"
        val isCredit = lowerContent.contains("sent you") ||
                lowerContent.contains("credited") ||
                lowerContent.contains("received") ||
                lowerContent.contains("deposited") ||
                lowerContent.contains("refund") ||
                lowerContent.contains("cashback") ||
                (lowerContent.contains("to your account") && (lowerContent.contains("sent") || lowerContent.contains("transferred")))

        if (isCredit) {
            return TransactionType.CREDIT
        }

        // High-confidence DEBIT patterns:
        // "Paid to...", "debited from...", "debited for...", "transferred to...", "spent on...", "sent ₹... to..."
        val isDebit = lowerContent.contains("debited") ||
                lowerContent.contains("paid") ||
                lowerContent.contains("spent") ||
                lowerContent.contains("purchase") ||
                lowerContent.contains("transferred to") ||
                Pattern.compile("""\bsent\s+(?:[₹]|rs|inr)?\s*[\d,.]*\s*to\b""").matcher(lowerContent).find() ||
                (lowerContent.contains("sent") && !lowerContent.contains("sent you") && !lowerContent.contains("to your account"))

        if (isDebit) {
            return TransactionType.DEBIT
        }

        return null
    }

    private fun extractMerchant(title: String?, text: String?, fullContent: String, type: TransactionType): String {
        if (type == TransactionType.CREDIT) {
            // Check if title itself is a personal contact/sender name (e.g. GPay or PhonePe notification title)
            val tClean = title?.trim() ?: ""
            val lowerT = tClean.lowercase()
            val appIndicators = listOf("phonepe", "google", "paytm", "bhim", "bank", "alert", "sms", "money", "received", "credited", "payment", "upi")
            if (tClean.length in 2..35 && appIndicators.none { lowerT.contains(it) }) {
                val sanitizedTitle = sanitizeParty(tClean)
                if (sanitizedTitle.isNotBlank()) return sanitizedTitle
            }

            val creditPatterns = listOf(
                // Bank SMS: "credited ... by Rs 2,500.00 on 27-Sep-26 by VIKRAM SINGH (UPI...)"
                Pattern.compile("""(?:credited.*?\s+by\s+(?:Rs\.?|INR)?\s*[\d,.]*.*?\s+by)\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // Parenthesized sender name: "from xxx@upi (Rahul Dravid)"
                Pattern.compile("""\((?:from\s+)?([A-Za-z\s]{3,30})\)""", Pattern.CASE_INSENSITIVE),
                // "Ramesh Kumar sent you ₹500" or "Akhil Reddy sent"
                Pattern.compile("""(?:^|[\n\r]|:\s*|\b(?:received|alert)\s+)?([A-Za-z0-9\s&'.-]{2,30}?)\s+(?:has\s+)?sent\b""", Pattern.CASE_INSENSITIVE),
                // "Received ₹500 from Ramesh Kumar"
                Pattern.compile("""(?:received.*?\s+from)\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "Payment of ₹1,200 received from Suresh Raina"
                Pattern.compile("""(?:payment.*?\s+from)\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "from Ramesh Kumar"
                Pattern.compile("""\bfrom\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|\(|$))""", Pattern.CASE_INSENSITIVE)
            )

            // Try matching in text first (avoids prepending title noise), then fullContent
            val candidates = listOfNotNull(text, fullContent)
            for (candidateText in candidates) {
                for (pattern in creditPatterns) {
                    val matcher = pattern.matcher(candidateText)
                    if (matcher.find()) {
                        val candidate = sanitizeParty(matcher.group(1) ?: "")
                        if (candidate.isNotBlank()) return candidate
                    }
                }
            }

            if (fullContent.contains("cashback", ignoreCase = true)) return "Cashback Reward"
            if (fullContent.contains("refund", ignoreCase = true)) return "Refund"

            return "Sender"
        } else {
            val debitPatterns = listOf(
                // "Paid ₹650 to Reliance Fresh"
                Pattern.compile("""(?:paid.*?\s+to)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:\.|\,|on|using|via|ref|upi|avl|bal|for|$))""", Pattern.CASE_INSENSITIVE),
                // "Sent ₹200 to Rohit"
                Pattern.compile("""(?:sent.*?\s+to)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:\.|\,|on|using|via|ref|upi|avl|bal|for|$))""", Pattern.CASE_INSENSITIVE),
                // "debited for APSRTC Bus Booking" / "transferred to..." / "spent on..."
                Pattern.compile("""(?:transferred to|spent on|debited for|at)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:\.|\,|on|using|via|ref|upi|avl|bal|for|$))""", Pattern.CASE_INSENSITIVE),
                // "to Merchant"
                Pattern.compile("""(?:to|towards)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:\.|\,|on|using|via|ref|upi|avl|bal|for|$))""", Pattern.CASE_INSENSITIVE)
            )

            for (pattern in debitPatterns) {
                val matcher = pattern.matcher(fullContent)
                if (matcher.find()) {
                    val candidate = sanitizeParty(matcher.group(1) ?: "")
                    if (candidate.isNotBlank()) return candidate
                }
            }

            return "Unknown Merchant"
        }
    }

    private fun sanitizeParty(raw: String): String {
        var candidate = raw.trim()
        // Strip trailing preposition noise
        candidate = candidate.replace(Regex("""(?i)\s+(?:via|using|on|upi|ref|bal|avl)$"""), "").trim()

        // Strip leading app names if accidentally captured
        val leadingApps = listOf("PhonePe", "Google Pay", "Paytm", "BHIM", "Bank SMS", "HDFC Bank", "SBI", "ICICI")
        for (app in leadingApps) {
            if (candidate.startsWith(app, ignoreCase = true)) {
                candidate = candidate.substring(app.length).trim()
            }
        }

        val lower = candidate.lowercase()
        val invalidTokens = setOf("your account", "account", "a/c", "inr", "rs", "rupees", "upi", "google pay", "phonepe", "paytm", "bank")
        if (invalidTokens.contains(lower) || candidate.length < 2 || candidate.equals("google", ignoreCase = true)) {
            return ""
        }
        return candidate
    }

    private fun resolveAppName(packageName: String): String {
        return when {
            packageName.contains("nbu.paisa") -> "Google Pay"
            packageName.contains("phonepe") -> "PhonePe"
            packageName.contains("paytm") -> "Paytm"
            packageName.contains("dreamplug") -> "CRED"
            packageName.contains("npci") -> "BHIM"
            packageName.contains("messaging") || packageName.contains("sms") -> "Bank SMS"
            else -> "Financial App"
        }
    }
}
