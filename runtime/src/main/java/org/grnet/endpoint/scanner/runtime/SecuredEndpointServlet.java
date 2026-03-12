package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.grnet.endpoint.scanner.runtime.services.SecuredEndpointService;

import java.io.IOException;

@WebServlet
public class SecuredEndpointServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json");

        var holder = CDI.current().select(EndpointMetadataHolder.class).get();

        var json = mapper.writeValueAsString(holder.getData());
        resp.getWriter().write(json);
    }
}
