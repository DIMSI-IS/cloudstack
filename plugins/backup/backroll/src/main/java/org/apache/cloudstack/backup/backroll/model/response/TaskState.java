// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.backup.backroll.model.response;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskState {
    public static final String PENDING = "PENDING";
    public static final String FAILURE = "FAILURE";
    public static final String SUCCESS = "SUCCESS";
    public static final String PROGRESS = "PROGRESS";

    /**
     * Reads the "state" field of a Backroll task response.
     * Looking at the "state" field only avoids mistaking a response for a pending or failed one
     * just because a state name appears somewhere else in the payload (VM name, backup name...).
     * When a task failed, Backroll answers with an HTTP error status and the state is nested in a "detail" object.
     * @return the state, or an empty string when the body is not a JSON object or has no state
     */
    public static String of(final String json) {
        if (json == null) {
            return "";
        }
        try {
            final JSONObject body = new JSONObject(json);
            if (body.has("state")) {
                return body.optString("state");
            }
            final JSONObject detail = body.optJSONObject("detail");
            return detail == null ? "" : detail.optString("state");
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * @return true if the task is not finished yet
     */
    public static boolean isRunning(final String state) {
        return PENDING.equals(state) || PROGRESS.equals(state);
    }
}
