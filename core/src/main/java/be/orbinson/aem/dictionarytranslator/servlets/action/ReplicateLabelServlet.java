package be.orbinson.aem.dictionarytranslator.servlets.action;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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
import java.util.Iterator;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceSuperType = "granite/ui/components/coral/foundation/form",
        resourceTypes = "aem-dictionary-translator/servlet/action/publish-label",
        methods = "POST"
)
public class ReplicateLabelServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicateLabelServlet.class);

    @Reference
    private transient Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String labels = request.getParameter("labels");

        if (StringUtils.isEmpty(labels)) {
            LOG.warn("Labels parameters are required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                for (String label : labels.split(",")) {
                    //Splitting label into dictionary path and label key
                    label = label.replace("/mnt/dictionary", "");
                    int lastIndexOfBackslash = label.lastIndexOf('/');
                    String parentPath = "";
                    if (lastIndexOfBackslash != -1) {
                        parentPath = label.substring(0, lastIndexOfBackslash);
                        label = label.substring(lastIndexOfBackslash + 1);
                    }
                    ResourceResolver resourceResolver = getResourceResolver(request);
                    Iterator<Resource> iterator = getResources(resourceResolver, parentPath, label);
                    if (iterator.hasNext()) {
                        while (iterator.hasNext()) {
                            Resource resource = iterator.next();
                            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, resource.getPath());
                            LOG.debug("Published label on path '{}'", resource.getPath());
                        }
                    } else {
                        HtmlResponse htmlResponse = new HtmlResponse();
                        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, String.format("Unable to get label '%s", label));
                        htmlResponse.send(response, true);
                    }

                }
            } catch (ReplicationException e) {
                HtmlResponse htmlResponse = new HtmlResponse();
                htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while replicating label: " + e);
                htmlResponse.send(response, true);
            }
        }
    }

    @NotNull
    ResourceResolver getResourceResolver(SlingHttpServletRequest request) {
        return request.getResourceResolver();
    }

    @NotNull
    Iterator<Resource> getResources(ResourceResolver resolver, String parentPath, String label) {
        String query = "/jcr:root" + parentPath + "//element(*, mix:language)/" + label;
        return resolver.findResources(query, "xpath");
    }

}
