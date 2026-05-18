package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import com.fasterxml.jackson.annotation.JsonGetter;
import jakarta.enterprise.inject.spi.CDI;

import java.util.Collections;
import java.util.List;

public class GroupUser {

    public String id;

    public String username;
    public String firstName;
    public String lastName;

    public String email;

    public Attributes attributes;



    @JsonGetter("uid")
    public String getUid() {
        if (attributes == null || attributes.uid == null || attributes.uid.isEmpty()) {
            return null;
        }
        return attributes.uid.get(0);
    }
}
