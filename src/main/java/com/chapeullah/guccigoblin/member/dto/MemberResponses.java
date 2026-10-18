package com.chapeullah.guccigoblin.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MemberResponses(@JsonProperty("items") List<MemberResponse> memberList) {}
