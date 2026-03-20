package org.grnet.endpoint.scanner.runtime.endpoints;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="PageLink", description="An object represents the links of paginated entities.")
public class PageLink {

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "Uri to paginated entities.",
            example = "http://localhost:8080/v1/entities?page=1&size=10"
    )
    public String href;

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "Descriptor for how the target resource relates to the current resource.",
            example = "first"
    )
    public String rel;

    public PageLink() {}

    public PageLink(String href, String rel) {
        this.href = href;
        this.rel = rel;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }
}
