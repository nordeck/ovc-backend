package net.nordeck.ovc.backend;

/*
 * Copyright 2025 Nordeck IT + Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Constants
{
    public static final String DATE_TIME_ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static final String FREQUENCY_CUSTOM = "CUSTOM";
    public static final String FREQUENCY_ONCE = "ONCE";

    public static final String NO_MEETING_FOUND_FOR_ID = "No meeting found for id <%s>";
    public static final String NO_PARENT_MEETING_FOUND_FOR_ID = "No parent meeting found for id <%s>.";
    public static final String NO_NEXT_MEETING_FOUND_FOR_PARENT_ID = "No next meeting found for parent id <%s>";
    public static final String ERR_CHANGE_SINGLE_IN_SERIES = "The recurrence of a single meeting must be changed in the defined series.";
    public static final String ERR_CHANGE_END_TIME_IN_SERIES = "The end time of a single meeting must be after the one defined in the series.";
    public static final String ERR_WRONG_MEETING_PASSWORD = "Wrong meeting password.";
    public static final String ERR_PARTICIPANT_DUPLICATE = "Participant already exists with email <%s>.";
    public static final String ERR_NO_PARTICIPANT_FOUND_FOR_ID = "No participant found for id <%s>.";

    public static final String MEMBER = "member";
    public static final String OWNER = "owner";
    public static final String GAST = "Gast";
    public static final String APP_ISSUER = "ovc-backend";

    public static final String REQUEST_METHOD = "request_method";
    public static final String REQUEST_URI = "request_uri";
    public static final String REQUEST_QUERY = "request_query";
    public static final String AUTH_USER = "auth_user";
    public static final String INFO_MESSAGE = "info_message";
    public static final String ID = "id";
    public static final String REQUEST_BODY = "request_body";
}