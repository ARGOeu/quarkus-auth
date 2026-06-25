package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import java.util.List;
import java.util.Map;

public class GroupUserResponse {

    public String id;

    public String username;
    public String firstName;
    public String lastName;

    public String email;

    public String uid;

    public Map<String, List<UserGroupInfoDto>> memberships;
}