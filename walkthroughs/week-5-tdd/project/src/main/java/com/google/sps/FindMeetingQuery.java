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

    long meetingDuration = request.getDuration();
    Collection<String> mandatoryAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    List<String> allAttendees = new ArrayList<>();
    allAttendees.addAll(mandatoryAttendees);
    allAttendees.addAll(optionalAttendees);
    // If there are time slots that both mandatory and optional attendees are available, return
    // those time; otherwise, return time that fit just the mandatory attendees.
    List<TimeRange> availableTimes = getAvailableTimes(getNoOverlapTimes(getAttendeesUnavailableTimes(events, allAttendees)), meetingDuration);
    if (!availableTimes.isEmpty()) {
      return availableTimes;
    } else {
      return getAvailableTimes(getNoOverlapTimes(getAttendeesUnavailableTimes(events, mandatoryAttendees)), meetingDuration);
    }
  }

  /** Get a collection of all attendees' unavailable time ranges (due to other events). */
  private List<TimeRange> getAttendeesUnavailableTimes(
      Collection<Event> events, Collection<String> meetingAttendees) {
    List<TimeRange> unavailableTimes = new ArrayList<>();
    for (Event event : events) {
      Collection<String> eventAttendees = event.getAttendees();
      // If any attendee in the meeting joins that event as well, the time range of the event is
      // added to the unavailableTimes collection.
      if (!Collections.disjoint(meetingAttendees, eventAttendees)) {
        unavailableTimes.add(event.getWhen());
      }
    }
    return unavailableTimes;
  }

  /** Return a collection of time ranges in the time order without any overlap among each other. */
  private List<TimeRange> getNoOverlapTimes(List<TimeRange> unavailableTimes) {
    // "noOverlapTimes" is guaranteed to be sorted and merged without overlap in this function,
    // and it will keep being updated by "localNoOverlapTimes" when a new time is added
    // from the initial unsorted "unavailableTimes" list (with some overlaps).
    List<TimeRange> noOverlapTimes = new ArrayList<>();
    for (TimeRange newTime : unavailableTimes) {
      if (noOverlapTimes.isEmpty()) {
        noOverlapTimes.add(newTime);
        continue;
      }
      // Compare and combine the new time with any existing time range in collection in order.
      List<TimeRange> localNoOverlapTimes = new ArrayList<>();
      for (int i = 0; i < noOverlapTimes.size(); i++) {
        TimeRange existingTime = noOverlapTimes.get(i);
        // Case 1. Existing time is earlier than new time
        if (existingTime.end() <= newTime.start()) {
          localNoOverlapTimes.add(existingTime);
        } else if (existingTime.start() >= newTime.end()) {
          // Case 2. New time is earlier than existing time
          localNoOverlapTimes.add(newTime);
          localNoOverlapTimes.addAll(noOverlapTimes.subList(i, noOverlapTimes.size()));
          break;
        } else if (existingTime.contains(newTime)) {
          // Case 3. Existing time contains new time
          localNoOverlapTimes.addAll(noOverlapTimes.subList(i, noOverlapTimes.size()));
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
      if (localNoOverlapTimes.isEmpty()) {
        localNoOverlapTimes.add(newTime);
      } else {
        if (localNoOverlapTimes.get(localNoOverlapTimes.size() - 1).end() < newTime.end()) {
          localNoOverlapTimes.add(newTime);
        }
      }
      noOverlapTimes = localNoOverlapTimes;
    }
    return noOverlapTimes;
  }

  /** Get available times for meeting, given the unavailable time ranges and the meeting duration. */
  // TODO: Instead of sorting and merging time slots (i.e. eliminating overlaps) at the same time in
  // this function, I can sort first and then separately eliminate overlaps for the sorted time
  // ranges. The time complexity can be thus reduced.
  private List<TimeRange> getAvailableTimes(List<TimeRange> unavailableTimes, long duration) {
    List<TimeRange> availableTimes = new ArrayList<>();
    int start = TimeRange.START_OF_DAY;
    for (TimeRange eventTime : unavailableTimes) {
      if (start < eventTime.start()) {
        TimeRange meetingTime = TimeRange.fromStartEnd(start, eventTime.start(), false);
        if (checkEnoughSpace(meetingTime, duration)) {
          availableTimes.add(meetingTime);
        }
      }
      start = eventTime.end();
    }
    // Add the gap if the end time of the last event is not the end of the day.
    if (start < TimeRange.END_OF_DAY + 1) {
      TimeRange meetingTime = TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY + 1, false);
      if (checkEnoughSpace(meetingTime, duration)) {
        availableTimes.add(meetingTime);
      }
    }
    return availableTimes;
  }

  /** Check if a time range is not smaller than a given duration. */
  private boolean checkEnoughSpace(TimeRange time, long duration) {
    return time.contains(TimeRange.fromStartDuration(time.start(), (int) duration));
  }
}
