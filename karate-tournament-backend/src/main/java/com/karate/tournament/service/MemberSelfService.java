package com.karate.tournament.service;

import com.karate.tournament.dto.response.MemberAttendanceSummaryResponse;
import com.karate.tournament.dto.response.MemberClubProfileResponse;
import com.karate.tournament.dto.response.MemberFeeSummaryResponse;

public interface MemberSelfService {
  MemberClubProfileResponse clubProfile();
  MemberFeeSummaryResponse fees();
  MemberAttendanceSummaryResponse attendance();
}
