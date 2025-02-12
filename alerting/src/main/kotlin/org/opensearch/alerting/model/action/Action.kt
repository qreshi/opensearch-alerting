/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.alerting.model.action

import org.opensearch.common.UUIDs
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.ToXContentObject
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.script.Script
import java.io.IOException

/**
 * This class holds the data and parser logic for Action which is part of a trigger
 */
data class Action(
    val name: String,
    val destinationId: String,
    val subjectTemplate: Script?,
    val messageTemplate: Script,
    val throttleEnabled: Boolean,
    val throttle: Throttle?,
    val id: String = UUIDs.base64UUID()
) : Writeable, ToXContentObject {

    init {
        if (subjectTemplate != null) {
            require(subjectTemplate.lang == Action.MUSTACHE) { "subject_template must be a mustache script" }
        }
        require(messageTemplate.lang == Action.MUSTACHE) { "message_template must be a mustache script" }
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readString(), // name
        sin.readString(), // destinationId
        sin.readOptionalWriteable(::Script), // subjectTemplate
        Script(sin), // messageTemplate
        sin.readBoolean(), // throttleEnabled
        sin.readOptionalWriteable(::Throttle), // throttle
        sin.readString() // id
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        val xContentBuilder = builder.startObject()
            .field(ID_FIELD, id)
            .field(NAME_FIELD, name)
            .field(DESTINATION_ID_FIELD, destinationId)
            .field(MESSAGE_TEMPLATE_FIELD, messageTemplate)
            .field(THROTTLE_ENABLED_FIELD, throttleEnabled)
        if (subjectTemplate != null) {
            xContentBuilder.field(SUBJECT_TEMPLATE_FIELD, subjectTemplate)
        }
        if (throttle != null) {
            xContentBuilder.field(THROTTLE_FIELD, throttle)
        }
        return xContentBuilder.endObject()
    }

    fun asTemplateArg(): Map<String, Any> {
        return mapOf(NAME_FIELD to name)
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeString(name)
        out.writeString(destinationId)
        if (subjectTemplate != null) {
            out.writeBoolean(true)
            subjectTemplate.writeTo(out)
        } else {
            out.writeBoolean(false)
        }
        messageTemplate.writeTo(out)
        out.writeBoolean(throttleEnabled)
        if (throttle != null) {
            out.writeBoolean(true)
            throttle.writeTo(out)
        } else {
            out.writeBoolean(false)
        }
        out.writeString(id)
    }

    companion object {
        const val ID_FIELD = "id"
        const val NAME_FIELD = "name"
        const val DESTINATION_ID_FIELD = "destination_id"
        const val SUBJECT_TEMPLATE_FIELD = "subject_template"
        const val MESSAGE_TEMPLATE_FIELD = "message_template"
        const val THROTTLE_ENABLED_FIELD = "throttle_enabled"
        const val THROTTLE_FIELD = "throttle"
        const val MUSTACHE = "mustache"
        const val SUBJECT = "subject"
        const val MESSAGE = "message"
        const val MESSAGE_ID = "messageId"

        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): Action {
            var id = UUIDs.base64UUID() // assign a default action id if one is not specified
            lateinit var name: String
            lateinit var destinationId: String
            var subjectTemplate: Script? = null // subject template could be null for some destinations
            lateinit var messageTemplate: Script
            var throttleEnabled = false
            var throttle: Throttle? = null

            XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()
                when (fieldName) {
                    ID_FIELD -> id = xcp.text()
                    NAME_FIELD -> name = xcp.textOrNull()
                    DESTINATION_ID_FIELD -> destinationId = xcp.textOrNull()
                    SUBJECT_TEMPLATE_FIELD -> {
                        subjectTemplate = if (xcp.currentToken() == XContentParser.Token.VALUE_NULL) null else
                            Script.parse(xcp, Script.DEFAULT_TEMPLATE_LANG)
                    }
                    MESSAGE_TEMPLATE_FIELD -> messageTemplate = Script.parse(xcp, Script.DEFAULT_TEMPLATE_LANG)
                    THROTTLE_FIELD -> {
                        throttle = if (xcp.currentToken() == XContentParser.Token.VALUE_NULL) null else Throttle.parse(xcp)
                    }
                    THROTTLE_ENABLED_FIELD -> {
                        throttleEnabled = xcp.booleanValue()
                    }

                    else -> {
                        throw IllegalStateException("Unexpected field: $fieldName, while parsing action")
                    }
                }
            }

            if (throttleEnabled) {
                requireNotNull(throttle, { "Action throttle enabled but not set throttle value" })
            }

            return Action(
                requireNotNull(name) { "Action name is null" },
                requireNotNull(destinationId) { "Destination id is null" },
                subjectTemplate,
                requireNotNull(messageTemplate) { "Action message template is null" },
                throttleEnabled,
                throttle,
                id = requireNotNull(id)
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): Action {
            return Action(sin)
        }
    }
}
