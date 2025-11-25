package org.grnet.auth.entitlements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EntitlementUtils {

    private static final Pattern PATTERN =
            Pattern.compile(".*group:([^:]+)(?::((?:[^:]+:)*[^:]+))?:role=([^:]+)$");

    public static Entitlement parse(String raw) {
        var m = PATTERN.matcher(raw);
        if (!m.matches()) return null;

        var group = m.group(1);
        var hierarchyPart = m.group(2);
        var hierarchy = new ArrayList<String>();
        if (hierarchyPart != null) {
            for (var s : hierarchyPart.split(":")) if (!s.isEmpty()) hierarchy.add(s);
        }
        var role = m.group(3);
        return new Entitlement(group, hierarchy, role, raw);
    }

    public static List<Entitlement> parseEntitlements(List<String> raws) {
        var list = new ArrayList<Entitlement>();
        for (var r : raws) {
            var e = parse(r);
            if (e != null) list.add(e);
        }
        return list;
    }
}
