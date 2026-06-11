package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.Side;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record KataVoteResponse(
    int judgeNumber,
    Side side,
    BigDecimal voteValue
) {
}
