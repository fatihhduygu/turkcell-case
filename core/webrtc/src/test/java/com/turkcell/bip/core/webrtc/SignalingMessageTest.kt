package com.turkcell.bip.core.webrtc

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalingMessageTest {

    private val gson = Gson()

    @Test
    fun `CALL_REQUEST serializes and deserializes correctly`() {
        val original = SignalingMessage(SignalingMessageType.CALL_REQUEST)
        val json = gson.toJson(original)
        val decoded = gson.fromJson(json, SignalingMessage::class.java)
        assertEquals(SignalingMessageType.CALL_REQUEST, decoded.type)
        assertNull(decoded.data)
    }

    @Test
    fun `OFFER serializes sdp string as data`() {
        val sdp = "v=0\r\no=- 12345 IN IP4 127.0.0.1\r\n"
        val original = SignalingMessage(SignalingMessageType.OFFER, sdp)
        val json = gson.toJson(original)
        val decoded = gson.fromJson(json, SignalingMessage::class.java)
        assertEquals(SignalingMessageType.OFFER, decoded.type)
        assertEquals(sdp, decoded.data)
    }

    @Test
    fun `ANSWER serializes sdp string as data`() {
        val sdp = "v=0\r\na=recvonly\r\n"
        val original = SignalingMessage(SignalingMessageType.ANSWER, sdp)
        val json = gson.toJson(original)
        val decoded = gson.fromJson(json, SignalingMessage::class.java)
        assertEquals(SignalingMessageType.ANSWER, decoded.type)
        assertEquals(sdp, decoded.data)
    }

    @Test
    fun `all SignalingMessageType values round-trip`() {
        SignalingMessageType.entries.forEach { type ->
            val msg = SignalingMessage(type, "payload")
            val json = gson.toJson(msg)
            val decoded = gson.fromJson(json, SignalingMessage::class.java)
            assertEquals(type, decoded.type)
        }
    }

    @Test
    fun `END_CALL message has null data by default`() {
        val msg = SignalingMessage(SignalingMessageType.END_CALL)
        assertNull(msg.data)
    }
}
