package org.grnet.endpoint.scanner.runtime.entities.pagination;

import java.util.List;

public interface PageQuery<Entity> {

    int pageCount();

    Page page();

    long count();

    <T extends Entity> List<T> list();

    boolean hasPreviousPage();

    boolean hasNextPage();
}