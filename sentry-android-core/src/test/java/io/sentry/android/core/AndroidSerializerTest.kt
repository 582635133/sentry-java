package io.sentry.android.core

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LinkedTreeMap
import com.nhaarman.mockitokotlin2.mock
import io.sentry.core.DateUtils
import io.sentry.core.SentryEvent
import io.sentry.core.SentryLevel
import io.sentry.core.protocol.Contexts
import io.sentry.core.protocol.Device
import java.io.StringWriter
import java.util.TimeZone
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidSerializerTest {

    private val serializer = AndroidSerializer(mock())

    private fun serializeToString(ev: SentryEvent): String {
        val wrt = StringWriter()
        serializer.serialize(ev, wrt)
        return wrt.toString()
    }

    @Test
    fun `when serializing SentryEvent-SentryId object, it should become a event_id json without dashes`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.timestamp = null

        val actual = serializeToString(sentryEvent)

        val expected = "{\"event_id\":\"${sentryEvent.eventId}\"}"

        assertEquals(expected, actual)
    }

    @Test
    fun `when deserializing event_id, it should become a SentryEvent-SentryId uuid`() {
        val expected = UUID.randomUUID().toString().replace("-", "")
        val jsonEvent = "{\"event_id\":\"$expected\"}"

        val actual = serializer.deserializeEvent(jsonEvent)

        assertEquals(expected, actual.eventId.toString())
    }

    @Test
    fun `when serializing SentryEvent-Date, it should become a timestamp json ISO format`() {
        val sentryEvent = generateEmptySentryEvent()
        val dateIsoFormat = "2000-12-31T23:59:58Z"
        sentryEvent.eventId = null
        sentryEvent.timestamp = DateUtils.getDateTime(dateIsoFormat)

        val expected = "{\"timestamp\":\"$dateIsoFormat\"}"

        val actual = serializeToString(sentryEvent)

        assertEquals(expected, actual)
    }

    @Test
    fun `when deserializing timestamp, it should become a SentryEvent-Date`() {
        val sentryEvent = generateEmptySentryEvent()
        val dateIsoFormat = "2000-12-31T23:59:58Z"
        sentryEvent.eventId = null
        val expected = DateUtils.getDateTime(dateIsoFormat)
        sentryEvent.timestamp = expected

        val jsonEvent = "{\"timestamp\":\"$dateIsoFormat\"}"

        val actual = serializer.deserializeEvent(jsonEvent)

        assertEquals(expected, actual.timestamp)
    }

    @Test
    fun `when deserializing unknown properties, it should be added to unknown field`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val jsonEvent = "{\"string\":\"test\",\"int\":1,\"boolean\":true}"

        val actual = serializer.deserializeEvent(jsonEvent)

        assertEquals("test", (actual.unknown["string"] as JsonPrimitive).asString)
        assertEquals(1, (actual.unknown["int"] as JsonPrimitive).asInt)
        assertEquals(true, (actual.unknown["boolean"] as JsonPrimitive).asBoolean)
    }

    @Test
    fun `when deserializing unknown properties with nested objects, it should be added to unknown field`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val objects = hashMapOf<String, Any>()
        objects["int"] = 1
        objects["boolean"] = true

        val unknown = hashMapOf<String, Any>()
        unknown["object"] = objects
        sentryEvent.acceptUnknownProperties(unknown)

        val jsonEvent = "{\"object\":{\"int\":1,\"boolean\":true}}"

        val actual = serializer.deserializeEvent(jsonEvent)

        val hashMapActual = actual.unknown["object"] as JsonObject // gson creates it as JsonObject

        assertEquals(true, hashMapActual.get("boolean").asBoolean)
        assertEquals(1, (hashMapActual.get("int")).asInt)
    }

    @Test
    fun `when serializing unknown field, it should become unknown as json format`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val objects = hashMapOf<String, Any>()
        objects["int"] = 1
        objects["boolean"] = true

        val unknown = hashMapOf<String, Any>()
        unknown["object"] = objects

        sentryEvent.acceptUnknownProperties(unknown)

        val actual = serializeToString(sentryEvent)

        val expected = "{\"unknown\":{\"object\":{\"boolean\":true,\"int\":1}}}"

        assertEquals(expected, actual)
    }

    @Test
    fun `when serializing a TimeZone, it should become a timezone ID string`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null
        val device = Device()
        device.timezone = TimeZone.getTimeZone("Europe/Vienna")
        val contexts = Contexts()
        contexts.device = device
        sentryEvent.contexts = contexts

        val expected = "{\"contexts\":{\"device\":{\"timezone\":\"Europe/Vienna\"}}}"

        val actual = serializeToString(sentryEvent)

        assertEquals(expected, actual)
    }

    @Test
    fun `when deserializing a timezone ID string, it should become a Device-TimeZone`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val jsonEvent = "{\"contexts\":{\"device\":{\"timezone\":\"Europe/Vienna\"}}}"

        val actual = serializer.deserializeEvent(jsonEvent)

        assertEquals("Europe/Vienna", (actual.contexts["device"] as LinkedTreeMap<*, *>)["timezone"]) // TODO: fix it when casting is being done proerly
    }

    @Test
    fun `when serializing a DeviceOrientation, it should become an orientation string`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null
        val device = Device()
        device.orientation = Device.DeviceOrientation.LANDSCAPE
        val contexts = Contexts()
        contexts.device = device
        sentryEvent.contexts = contexts

        val expected = "{\"contexts\":{\"device\":{\"orientation\":\"landscape\"}}}"

        val actual = serializeToString(sentryEvent)

        assertEquals(expected, actual)
    }

    @Test
    fun `when deserializing an orientation string, it should become a DeviceOrientation`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val jsonEvent = "{\"contexts\":{\"device\":{\"orientation\":\"landscape\"}}}"

        val actual = serializer.deserializeEvent(jsonEvent)

        val orientation = (actual.contexts["device"] as LinkedTreeMap<*, *>)["orientation"] as String // TODO: fix it when casting is being done proerly

        assertEquals(Device.DeviceOrientation.LANDSCAPE, Device.DeviceOrientation.valueOf(orientation.toUpperCase())) // here too
    }

    @Test
    fun `when serializing a SentryLevel, it should become a sentry level string`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null
        sentryEvent.level = SentryLevel.DEBUG

        val expected = "{\"level\":\"debug\"}"

        val actual = serializeToString(sentryEvent)

        assertEquals(expected, actual)
    }

    @Test
    fun `when deserializing a sentry level string, it should become a SentryLevel`() {
        val sentryEvent = generateEmptySentryEvent()
        sentryEvent.eventId = null
        sentryEvent.timestamp = null

        val jsonEvent = "{\"level\":\"debug\"}"

        val actual = serializer.deserializeEvent(jsonEvent)

        assertEquals(SentryLevel.DEBUG, actual.level)
    }

    @Test
    fun `when theres a null value, gson wont blow up`() {
        val json = FileFromResources.invoke("event.json")
        val event = serializer.deserializeEvent(json)
        assertNull(event.user)
    }

    private fun generateEmptySentryEvent(): SentryEvent {
        return SentryEvent().apply {
            contexts = null
        }
    }
}