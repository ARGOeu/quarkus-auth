package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import java.util.List;

public class GroupUserResponse {

    public String id;

    public String username;
    public String firstName;
    public String lastName;

    public String email;

    public String uid;

    public List<UserGroupInfoDto> groups;
}