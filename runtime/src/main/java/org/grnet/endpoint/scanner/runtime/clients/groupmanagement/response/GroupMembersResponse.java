package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersResponse {

    public List<GroupMemberEntry> results = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long count = 0L;
}
