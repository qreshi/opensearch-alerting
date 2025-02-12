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

package org.opensearch.alerting.model

import org.opensearch.alerting.builder
import org.opensearch.alerting.elasticapi.string
import org.opensearch.alerting.model.action.Action
import org.opensearch.alerting.model.action.Throttle
import org.opensearch.alerting.model.destination.email.EmailAccount
import org.opensearch.alerting.model.destination.email.EmailGroup
import org.opensearch.alerting.parser
import org.opensearch.alerting.randomAction
import org.opensearch.alerting.randomActionExecutionResult
import org.opensearch.alerting.randomAlert
import org.opensearch.alerting.randomEmailAccount
import org.opensearch.alerting.randomEmailGroup
import org.opensearch.alerting.randomMonitor
import org.opensearch.alerting.randomMonitorWithoutUser
import org.opensearch.alerting.randomThrottle
import org.opensearch.alerting.randomTrigger
import org.opensearch.alerting.randomUser
import org.opensearch.alerting.randomUserEmpty
import org.opensearch.alerting.toJsonString
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.commons.authuser.User
import org.opensearch.test.OpenSearchTestCase
import kotlin.test.assertFailsWith

class XContentTests : OpenSearchTestCase() {

    fun `test action parsing`() {
        val action = randomAction()
        val actionString = action.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedAction = Action.parse(parser(actionString))
        assertEquals("Round tripping Monitor doesn't work", action, parsedAction)
    }

    fun `test action parsing with null subject template`() {
        val action = randomAction().copy(subjectTemplate = null)
        val actionString = action.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedAction = Action.parse(parser(actionString))
        assertEquals("Round tripping Monitor doesn't work", action, parsedAction)
    }

    fun `test action parsing with null throttle`() {
        val action = randomAction().copy(throttle = null)
        val actionString = action.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedAction = Action.parse(parser(actionString))
        assertEquals("Round tripping Monitor doesn't work", action, parsedAction)
    }

    fun `test action parsing with throttled enabled and null throttle`() {
        val action = randomAction().copy(throttle = null).copy(throttleEnabled = true)
        val actionString = action.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        assertFailsWith<IllegalArgumentException>("Action throttle enabled but not set throttle value") {
            Action.parse(parser(actionString))
        }
    }

    fun `test throttle parsing`() {
        val throttle = randomThrottle()
        val throttleString = throttle.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedThrottle = Throttle.parse(parser(throttleString))
        assertEquals("Round tripping Monitor doesn't work", throttle, parsedThrottle)
    }

    fun `test throttle parsing with wrong unit`() {
        val throttle = randomThrottle()
        val throttleString = throttle.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val wrongThrottleString = throttleString.replace("MINUTES", "wrongunit")

        assertFailsWith<IllegalArgumentException>("Only support MINUTES throttle unit") { Throttle.parse(parser(wrongThrottleString)) }
    }

    fun `test throttle parsing with negative value`() {
        val throttle = randomThrottle().copy(value = -1)
        val throttleString = throttle.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()

        assertFailsWith<IllegalArgumentException>("Can only set positive throttle period") { Throttle.parse(parser(throttleString)) }
    }

    fun `test monitor parsing`() {
        val monitor = randomMonitor()

        val monitorString = monitor.toJsonString()
        val parsedMonitor = Monitor.parse(parser(monitorString))
        assertEquals("Round tripping Monitor doesn't work", monitor, parsedMonitor)
    }

    fun `test trigger parsing`() {
        val trigger = randomTrigger()

        val triggerString = trigger.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedTrigger = Trigger.parse(parser(triggerString))

        assertEquals("Round tripping Trigger doesn't work", trigger, parsedTrigger)
    }

    fun `test alert parsing`() {
        val alert = randomAlert()

        val alertString = alert.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedAlert = Alert.parse(parser(alertString))

        assertEquals("Round tripping alert doesn't work", alert, parsedAlert)
    }

    fun `test alert parsing without user`() {
        val alertStr = "{\"id\":\"\",\"version\":-1,\"monitor_id\":\"\",\"schema_version\":0,\"monitor_version\":1," +
            "\"monitor_name\":\"ARahqfRaJG\",\"trigger_id\":\"fhe1-XQBySl0wQKDBkOG\",\"trigger_name\":\"ffELMuhlro\"," +
            "\"state\":\"ACTIVE\",\"error_message\":null,\"alert_history\":[],\"severity\":\"1\",\"action_execution_results\"" +
            ":[{\"action_id\":\"ghe1-XQBySl0wQKDBkOG\",\"last_execution_time\":1601917224583,\"throttled_count\":-1478015168}," +
            "{\"action_id\":\"gxe1-XQBySl0wQKDBkOH\",\"last_execution_time\":1601917224583,\"throttled_count\":-768533744}]," +
            "\"start_time\":1601917224599,\"last_notification_time\":null,\"end_time\":null,\"acknowledged_time\":null}"
        val parsedAlert = Alert.parse(parser(alertStr))
        assertNull(parsedAlert.monitorUser)
    }

    fun `test alert parsing with user as null`() {
        val alertStr = "{\"id\":\"\",\"version\":-1,\"monitor_id\":\"\",\"schema_version\":0,\"monitor_version\":1,\"monitor_user\":null," +
            "\"monitor_name\":\"ARahqfRaJG\",\"trigger_id\":\"fhe1-XQBySl0wQKDBkOG\",\"trigger_name\":\"ffELMuhlro\"," +
            "\"state\":\"ACTIVE\",\"error_message\":null,\"alert_history\":[],\"severity\":\"1\",\"action_execution_results\"" +
            ":[{\"action_id\":\"ghe1-XQBySl0wQKDBkOG\",\"last_execution_time\":1601917224583,\"throttled_count\":-1478015168}," +
            "{\"action_id\":\"gxe1-XQBySl0wQKDBkOH\",\"last_execution_time\":1601917224583,\"throttled_count\":-768533744}]," +
            "\"start_time\":1601917224599,\"last_notification_time\":null,\"end_time\":null,\"acknowledged_time\":null}"
        val parsedAlert = Alert.parse(parser(alertStr))
        assertNull(parsedAlert.monitorUser)
    }

    fun `test action execution result parsing`() {
        val actionExecutionResult = randomActionExecutionResult()

        val actionExecutionResultString = actionExecutionResult.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedActionExecutionResultString = ActionExecutionResult.parse(parser(actionExecutionResultString))

        assertEquals("Round tripping alert doesn't work", actionExecutionResult, parsedActionExecutionResultString)
    }

    fun `test creating a monitor with duplicate trigger ids fails`() {
        try {
            val repeatedTrigger = randomTrigger()
            randomMonitor().copy(triggers = listOf(repeatedTrigger, repeatedTrigger))
            fail("Creating a monitor with duplicate triggers did not fail.")
        } catch (ignored: IllegalArgumentException) {
        }
    }

    fun `test user parsing`() {
        val user = randomUser()
        val userString = user.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()
        val parsedUser = User.parse(parser(userString))
        assertEquals("Round tripping user doesn't work", user, parsedUser)
    }

    fun `test empty user parsing`() {
        val user = randomUserEmpty()
        val userString = user.toXContent(builder(), ToXContent.EMPTY_PARAMS).string()

        val parsedUser = User.parse(parser(userString))
        assertEquals("Round tripping user doesn't work", user, parsedUser)
        assertEquals("", parsedUser.name)
        assertEquals(0, parsedUser.roles.size)
    }

    fun `test monitor parsing without user`() {
        val monitor = randomMonitorWithoutUser()

        val monitorString = monitor.toJsonString()
        val parsedMonitor = Monitor.parse(parser(monitorString))
        assertEquals("Round tripping Monitor doesn't work", monitor, parsedMonitor)
        assertNull(parsedMonitor.user)
    }

    fun `test email account parsing`() {
        val emailAccount = randomEmailAccount()

        val emailAccountString = emailAccount.toJsonString()
        val parsedEmailAccount = EmailAccount.parse(parser(emailAccountString))
        assertEquals("Round tripping EmailAccount doesn't work", emailAccount, parsedEmailAccount)
    }

    fun `test email group parsing`() {
        val emailGroup = randomEmailGroup()

        val emailGroupString = emailGroup.toJsonString()
        val parsedEmailGroup = EmailGroup.parse(parser(emailGroupString))
        assertEquals("Round tripping EmailGroup doesn't work", emailGroup, parsedEmailGroup)
    }
}
