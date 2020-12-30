// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // If duration is longer than a day, no option for this case.
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }
    // Get general available time ranges.
    Collection<String> attendees = request.getAttendees();
    List<TimeRange> unavailableTime =
        getNoOverlapTime(getAttendeesUnavailableTime(events, attendees));
    List<TimeRange> availableTime = getAvailableTime(unavailableTime, request.getDuration());
    return availableTime;
  }

  /** Get a collection of all attendees' unavailable time ranges (due to other events). */
  private List<TimeRange> getAttendeesUnavailableTime(
      Collection<Event> events, Collection<String> meetingAttendees) {
    List<TimeRange> unavailableTime = new ArrayList<>();
    for (Event event : events) {
      Collection<String> eventAttendees = event.getAttendees();
      // If any attendee in the meeting joins that event as well, the time range of the event is
      // added to the unavailableTime collection.
      if (!Collections.disjoint(meetingAttendees, eventAttendees)) {
        unavailableTime.add(event.getWhen());
      }
    }
    return unavailableTime;
  }

  /** Return a collection of time ranges in the time order without any overlap among each other. */
  private List<TimeRange> getNoOverlapTime(List<TimeRange> unavailableTime) {
    List<TimeRange> noOverlapTime = new ArrayList<>();
    for (TimeRange newTime : unavailableTime) {
      if (noOverlapTime.isEmpty()) {
        noOverlapTime.add(newTime);
        continue;
      }
      // Compare and combine the new time with any existing time range in collection in order.
      List<TimeRange> localNoOverlapTime = new ArrayList<>();
      for (int i = 0; i < noOverlapTime.size(); i++) {
        TimeRange existingTime = noOverlapTime.get(i);
        // Case 1. Existing time is earlier than new time
        if (existingTime.end() <= newTime.start()) {
          localNoOverlapTime.add(existingTime);
        } else if (existingTime.start() >= newTime.end()) {
          // Case 2. New time is earlier than existing time
          localNoOverlapTime.add(newTime);
          localNoOverlapTime.addAll(noOverlapTime.subList(i, noOverlapTime.size()));
          break;
        } else if (existingTime.contains(newTime)) {
          // Case 3. Existing time contains new time
          localNoOverlapTime.addAll(noOverlapTime.subList(i, noOverlapTime.size()));
          break;
        } else if (newTime.contains(existingTime)) {
          // Case 4. New time contains existing time
        } else {
          // Case 5. Overlap (no contain) exists between existing time and new time
          newTime = TimeRange.fromStartEnd(Math.min(newTime.start(), existingTime.start()),
              Math.max(newTime.end(), existingTime.end()), false);
        }
      }
      // Check whether newTime has never been included because it is the last one.
      if (localNoOverlapTime.isEmpty()) {
        localNoOverlapTime.add(newTime);
      } else {
        if (localNoOverlapTime.get(localNoOverlapTime.size() - 1).end() < newTime.end()) {
          localNoOverlapTime.add(newTime);
        }
      }
      noOverlapTime = localNoOverlapTime;
    }
    return noOverlapTime;
  }

  /** Get available time for meeting, given the unavailable time ranges and the meeting duration. */
  private List<TimeRange> getAvailableTime(List<TimeRange> unavailableTime, long duration) {
    List<TimeRange> availableTime = new ArrayList<>();
    int start = TimeRange.START_OF_DAY;
    for (TimeRange eventTime : unavailableTime) {
      if (start < eventTime.start()) {
        TimeRange meetingTime = TimeRange.fromStartEnd(start, eventTime.start(), false);
        if (checkEnoughSpace(meetingTime, duration)) {
          availableTime.add(meetingTime);
        }
      }
      start = eventTime.end();
    }
    // Add the gap if the end time of the last event is not the end of the day.
    if (start < TimeRange.END_OF_DAY + 1) {
      TimeRange meetingTime = TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY + 1, false);
      if (checkEnoughSpace(meetingTime, duration)) {
        availableTime.add(meetingTime);
      }
    }
    return availableTime;
  }

  /** Check if a time range is not smaller than a given duration. */
  private boolean checkEnoughSpace(TimeRange time, long duration) {
    return time.contains(TimeRange.fromStartDuration(time.start(), (int) duration));
  }
}
