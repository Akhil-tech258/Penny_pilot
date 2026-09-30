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
        "sip", "mutual fund", "portfolio", "stocks", "gold",
        "start a", "start your", "starting at", "starting from", "starting @", "starting ₹", "starting rs",
        "reward", "scratch card", "win ", "won ", "cashback of up to", "earn up to", "earn ₹", "earn rs",
        "pre-approved", "pre approved", "apply for", "apply now", "apply today", "kyc", "cibil", "credit score",
        "gift card", "spin the wheel", "spin & win", "voucher", "insurance", "save daily", "grow your wealth",
        "claim your", "congratulations", "bonus", "unlock", "explore"
    )

    private val UPI_REF_PATTERN = Pattern.compile(
        """\b(?:upi\s*ref(?:erence)?(?:\s*no)?[:\s/]*|utr[:\s/]*|ref\s*no[:\s/]*|txn\s*id[:\s/]*|ref[:\s/]*)\s*([0-9]{10,14})\b""",
        Pattern.CASE_INSENSITIVE
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
        // Don't filter if notification is a legitimate refund or cashback
        val isLegitRefundOrCashback = lowerContent.contains("refund") || lowerContent.contains("cashback")
        if (!isLegitRefundOrCashback) {
            for (ignore in IGNORE_KEYWORDS) {
                if (lowerContent.contains(ignore)) {
                    return DetectionResult(false)
                }
            }
            if (Regex("""\b(?:mutual\s+)?funds?\b""").containsMatchIn(lowerContent)) {
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

        // 5. Extract Reference / UTR number if present
        val refMatcher = UPI_REF_PATTERN.matcher(fullContent)
        val refNumber = if (refMatcher.find()) "Ref: ${refMatcher.group(1)}" else ""

        // 6. Resolve friendly source app name
        val sourceApp = resolveAppName(packageName)

        val transaction = Transaction(
            amount = amount,
            currency = "INR",
            type = type,
            merchant = merchant,
            description = refNumber,
            sourceApp = sourceApp,
            sourcePackage = packageName,
            transactionTime = System.currentTimeMillis(),
            detectedTime = System.currentTimeMillis(),
            status = TransactionStatus.PENDING
        )

        return DetectionResult(isTransaction = true, transaction = transaction)
    }

    private fun determineTransactionType(lowerContent: String): TransactionType? {
        val text = lowerContent.replace(Regex("""\s+"""), " ")

        // --- STEP 1: Explicit User Account DEBITS ---
        // If money left the user's account, it is strictly DEBIT, even if the notification contains "credited to merchant"
        val isExplicitDebit = text.contains("debited from") ||
                text.contains("debited by") ||
                text.contains("debited for") ||
                text.contains("debited with") ||
                text.contains("is debited") ||
                text.contains("was debited") ||
                text.contains("has been debited") ||
                Regex("""\b(?:a/c|acct|account)\s+.*?\bdebited\b""").containsMatchIn(text) ||
                Regex("""\bdebited\b.*?\b(?:a/c|acct|account)\b""").containsMatchIn(text)

        if (isExplicitDebit) {
            return TransactionType.DEBIT
        }

        // --- STEP 2: Explicit User Account CREDITS ---
        // Phrases indicating user's bank account or wallet received a deposit
        val isExplicitCredit = Regex("""\b(?:a/c|acct|account)\s+.*?\bcredited\b""").containsMatchIn(text) ||
                text.contains("credited to your a/c") ||
                text.contains("credited to your account") ||
                text.contains("credited with") ||
                text.contains("is credited") ||
                text.contains("has been credited") ||
                text.contains("deposited into your account") ||
                text.contains("deposited to your account") ||
                text.contains("deposited in your account") ||
                text.contains("received in your account") ||
                text.contains("received in your a/c")

        if (isExplicitCredit) {
            return TransactionType.CREDIT
        }

        // --- STEP 3: Contextual Merchant / P2P Directions ---
        // 3a. "received by <merchant>" means user paid money and merchant received it -> DEBIT!
        if (Regex("""\breceived\s+by\b""").containsMatchIn(text)) {
            return TransactionType.DEBIT
        }

        // 3b. Income / P2P Credits
        val isP2pCredit = text.contains("sent you") ||
                Regex("""\breceived\s+(?:[₹]|rs|inr)?\s*[\d,.]*\s+from\b""").containsMatchIn(text) ||
                Regex("""\bpayment\s+(?:of\s+.*?\s+)?received\s+from\b""").containsMatchIn(text) ||
                text.contains("refund of") ||
                text.contains("refund received") ||
                text.contains("cashback of") ||
                text.contains("cashback credited") ||
                text.contains("money added to")

        if (isP2pCredit) {
            return TransactionType.CREDIT
        }

        // 3c. Outflow / Payments / Debits
        val isP2pOrMerchantDebit = text.contains("paid to") ||
                text.contains("paid successfully") ||
                text.contains("you paid") ||
                text.contains("spent on") ||
                text.contains("spent at") ||
                text.contains("purchase at") ||
                text.contains("withdrawn from") ||
                text.contains("transferred to") ||
                Regex("""\bpayment\s+(?:of\s+.*?\s+)?to\b""").containsMatchIn(text) ||
                Regex("""\bsent\s+(?:[₹]|rs|inr)?\s*[\d,.]*\s*to\b""").containsMatchIn(text)

        if (isP2pOrMerchantDebit) {
            return TransactionType.DEBIT
        }

        // --- STEP 4: Fallback checks ---
        if (text.contains("debited") || text.contains("paid") || text.contains("spent")) {
            return TransactionType.DEBIT
        }

        if (text.contains("credited") || text.contains("cashback") || text.contains("refund")) {
            return TransactionType.CREDIT
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
                // "Payment of ₹250.00 received by Star Cafe"
                Pattern.compile("""(?:received\s+by)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "Paid ₹650 to Reliance Fresh"
                Pattern.compile("""(?:paid.*?\s+to)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "Sent ₹200 to Rohit"
                Pattern.compile("""(?:sent.*?\s+to)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "purchase at / spent at Amazon"
                Pattern.compile("""(?:purchase\s+at|spent\s+at|at)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "debited for APSRTC Bus Booking" / "transferred to..." / "spent on..."
                Pattern.compile("""(?:transferred\s+to|spent\s+on|debited\s+for)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE),
                // "to Merchant"
                Pattern.compile("""(?:to|towards)\s+([A-Za-z0-9\s&'-]{2,25}?)(?:\s*(?:[\n\r.,]|on\b|using\b|via\b|ref\b|upi\b|avl\b|bal\b|for\b|txn\b|\(|$))""", Pattern.CASE_INSENSITIVE)
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
        var candidate = raw.trim().trimEnd('.', ',', ':', ';', '-')
        // Strip trailing preposition noise
        candidate = candidate.replace(Regex("""(?i)\s+(?:via|using|on|upi|ref|bal|avl|txn)$"""), "").trim()

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
