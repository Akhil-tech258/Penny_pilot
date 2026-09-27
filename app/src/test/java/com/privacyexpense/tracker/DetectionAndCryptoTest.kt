package com.privacyexpense.tracker

import com.privacyexpense.tracker.data.model.TransactionType
import com.privacyexpense.tracker.data.security.CryptoManager
import com.privacyexpense.tracker.service.TransactionDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionAndCryptoTest {

    @Test
    fun testTransactionDetection_GooglePay() {
        val packageName = "com.google.android.apps.nbu.paisa.user"
        val title = "Google Pay"
        val text = "Paid ₹650.00 to Reliance Fresh. UPI Ref: 421098451234"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect transaction", result.isTransaction)
        assertNotNull(result.transaction)

        val tx = result.transaction!!
        assertEquals(650.0, tx.amount, 0.01)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("Reliance Fresh", tx.merchant)
        assertEquals("Google Pay", tx.sourceApp)
    }

    @Test
    fun testTransactionDetection_PhonePe() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe Alert"
        val text = "₹120.00 debited for APSRTC Bus Booking via UPI"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue(result.isTransaction)
        val tx = result.transaction!!
        assertEquals(120.0, tx.amount, 0.01)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("PhonePe", tx.sourceApp)
    }

    @Test
    fun testTransactionDetection_IgnorePromotional() {
        val packageName = "com.bank.app"
        val title = "Special Offer!"
        val text = "Use your debit card and get 10% discount on flight tickets"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertFalse("Promotional alerts should be ignored", result.isTransaction)
    }

    @Test
    fun testTransactionDetection_PhonePe_SIP_Promo() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Start your ₹10 SIP fund on PhonePe and grow your wealth"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertFalse("PhonePe SIP fund promo should be ignored as non-transaction", result.isTransaction)
    }

    @Test
    fun testTransactionDetection_InvestGold_Promo() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe Investment"
        val text = "Invest ₹10 in 24K Gold daily starting today!"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertFalse("Invest promo should be ignored", result.isTransaction)
    }

    @Test
    fun testTransactionDetection_Credit_SentYou() {
        val packageName = "com.google.android.apps.nbu.paisa.user"
        val title = "Google Pay"
        val text = "Abhi sent you ₹100 via UPI"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(100.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("Abhi", tx.merchant)
    }

    @Test
    fun testTransactionDetection_Credit_SentToYourAccount() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Abhi sent 1 rupee to your account credited to XXXXX acc via UPI"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect credit transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(1.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("Abhi", tx.merchant)
    }

    @Test
    fun testTransactionDetection_Credit_ReceivedFrom() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Received ₹500.00 from Ramesh Kumar using PhonePe"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect credit transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(500.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("Ramesh Kumar", tx.merchant)
    }

    @Test
    fun testTransactionDetection_Credit_MultiWord_SentYou() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Suresh Raina sent you ₹1,500.00 via UPI"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect credit transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(1500.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("Suresh Raina", tx.merchant)
    }

    @Test
    fun testTransactionDetection_Credit_TitleIsSender() {
        val packageName = "com.google.android.apps.nbu.paisa.user"
        val title = "Akhil Reddy"
        val text = "Sent you ₹750 via Google Pay"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect credit transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(750.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("Akhil Reddy", tx.merchant)
    }

    @Test
    fun testTransactionDetection_Credit_BankSms() {
        val packageName = "com.google.android.apps.messaging"
        val title = "Bank SMS"
        val text = "A/c *1234 credited by Rs 2,500.00 on 27-Sep-26 by VIKRAM SINGH UPI/12345/P2A"

        val result = TransactionDetectionEngine.parseNotification(packageName, title, text)
        assertTrue("Should detect credit transaction", result.isTransaction)
        val tx = result.transaction!!
        assertEquals(2500.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("VIKRAM SINGH", tx.merchant)
    }

    @Test
    fun testCryptoManager_PasswordHashAndVerify() {
        val password = "SuperSecretPassword123"
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPassword(password, salt)

        assertTrue(CryptoManager.verifyPassword(password, salt, hash))
        assertFalse(CryptoManager.verifyPassword("WrongPassword", salt, hash))
    }

    @Test
    fun testCryptoManager_EncryptionAndDecryption() {
        val sensitiveData = """{"transactions":[{"amount":500.0,"merchant":"Zara"}]}"""
        val password = "UserBackupPassword"
        val salt = CryptoManager.generateSalt()

        val encrypted = CryptoManager.encryptPayload(sensitiveData, password, salt)
        assertNotNull(encrypted)
        assertTrue(encrypted.contains(":"))

        val decrypted = CryptoManager.decryptPayload(encrypted, password, salt)
        assertEquals(sensitiveData, decrypted)
    }
}
