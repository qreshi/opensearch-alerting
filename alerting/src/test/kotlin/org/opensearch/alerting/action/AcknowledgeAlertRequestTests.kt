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
 *   Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.alerting.action

import org.junit.Assert
import org.opensearch.action.support.WriteRequest
import org.opensearch.common.io.stream.BytesStreamOutput
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.test.OpenSearchTestCase

class AcknowledgeAlertRequestTests : OpenSearchTestCase() {

    fun `test acknowledge alert request`() {
        val req = AcknowledgeAlertRequest("1234", mutableListOf("1", "2", "3", "4"), WriteRequest.RefreshPolicy.IMMEDIATE)
        Assert.assertNotNull(req)

        val out = BytesStreamOutput()
        req.writeTo(out)
        val sin = StreamInput.wrap(out.bytes().toBytesRef().bytes)
        val newReq = AcknowledgeAlertRequest(sin)
        Assert.assertEquals("1234", newReq.monitorId)
        Assert.assertEquals(4, newReq.alertIds.size)
        Assert.assertEquals(WriteRequest.RefreshPolicy.IMMEDIATE, newReq.refreshPolicy)
    }
}
