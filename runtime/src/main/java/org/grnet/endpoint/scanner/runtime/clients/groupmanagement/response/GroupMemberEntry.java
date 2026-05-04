package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import java.util.List;

public class GroupMemberEntry {

    public String id;
    public Group group;
    public GroupUser user;
    public List<String> groupRoles;
    public String status;
    public boolean direct;
}
