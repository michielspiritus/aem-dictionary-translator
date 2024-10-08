package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/delete-label",
        methods = "POST"
)
public class DeleteLabelServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteLabelServlet.class);
    public static final String LABELS_PARAM = "labels";

    @Reference
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String labels = request.getParameter(LABELS_PARAM);

        if (StringUtils.isEmpty(labels)) {
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Labels parameters are required");
            htmlResponse.send(response, true);
        } else {
            for (String label : labels.split(",")) {
                ResourceResolver resourceResolver = request.getResourceResolver();
                Resource resource = resourceResolver.getResource(label);
                try {
                    if (resource != null) {
                        // javasecurity:S5145
                        LOG.debug("Delete label on path '{}'", labels);
                        deactivateAndDelete(resourceResolver, resource);
                    } else {
                        // javasecurity:S5145
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get label '%s'", labels));
                        htmlResponse.send(response, true);
                    }
                } catch (PersistenceException | ReplicationException e) {
                    HtmlResponse htmlResponse = new HtmlResponse();
                    htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Unable to delete labels '%s': %s", labels, e.getMessage()));
                    htmlResponse.send(response, true);
                }
            }
        }
    }

    private void deactivateAndDelete(ResourceResolver resourceResolver, Resource resource) throws ReplicationException, PersistenceException {
        String[] labelPaths = resource.getValueMap().get("labelPaths", String[].class);
        if (labelPaths != null) {
            for (String labelPath : labelPaths) {
                replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, labelPath);
            }
        }
        resourceResolver.delete(resource);
    }

}
