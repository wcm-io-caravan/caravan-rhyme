package io.wcm.caravan.rhyme.examples.aemhalbrowser.it.rules;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.it.rules.Templates.HOMEPAGE_TEMPLATE_PATH;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;

/**
 * Creates a test site with a home page using the templates defined in this project.
 * This is a JUnit 4 rule that cleans up everything after after the test is executed.
 */
public class SiteRule extends ExternalResource {

  private static final Logger log = LoggerFactory.getLogger(SiteRule.class);

  private static final String LANGUAGE_ROOT_PATH = "/content/aem-hal-browser";
  private static final String SITE_ROOT_PATH = LANGUAGE_ROOT_PATH + "/en-integration-test";

  private final Instance ruleInstance;

  /**
   * @param ruleInstance Sling Testing rule instance
   */
  public SiteRule(Instance ruleInstance) {
    this.ruleInstance = ruleInstance;
  }

  @Override
  @SuppressWarnings("null")
  protected void before() throws ClientException {
    CQClient cqclient = getClient();

    // create language root folder (if it does not exist already)
    if (!cqclient.exists(LANGUAGE_ROOT_PATH)) {
      log.info("Create folder: {}", LANGUAGE_ROOT_PATH);
      cqclient.createFolder(ResourceUtil.getName(LANGUAGE_ROOT_PATH),
          ResourceUtil.getName(LANGUAGE_ROOT_PATH),
          ResourceUtil.getParent(LANGUAGE_ROOT_PATH));
    }

    // create homepage page (remove it if it exists already)
    log.info("Create homepage: {}", SITE_ROOT_PATH);
    if (cqclient.exists(SITE_ROOT_PATH)) {
      cqclient.deletePage(new String[] { SITE_ROOT_PATH }, true, false);
    }
    cqclient.createPage(ResourceUtil.getName(SITE_ROOT_PATH),
        ResourceUtil.getName(SITE_ROOT_PATH),
        ResourceUtil.getParent(SITE_ROOT_PATH), HOMEPAGE_TEMPLATE_PATH);

    // replicate to publish
    ReplicationClient replicationClient = cqclient.adaptTo(ReplicationClient.class);
    replicationClient.activate(SITE_ROOT_PATH);

  }

  @Override
  protected void after() {
    try {
      log.info("Remove site: {}", SITE_ROOT_PATH);
      getClient().deletePage(new String[] { SITE_ROOT_PATH }, true, false);
    }
    catch (ClientException ex) {
      log.error("Unable to delete the site", ex);
    }
  }

  /**
   * The client to use to create and delete this page. The default implementation creates a {@link CQClient}.
   * The default implementation also uses the default admin user.
   * @return The client to use to create and delete this page.
   * @throws ClientException if the client cannot be retrieved
   */
  @SuppressWarnings("null")
  protected CQClient getClient() throws ClientException {
    return ruleInstance.getAdminClient(CQClient.class);
  }

  /**
   * @return Site root path
   */
  public String getRootPath() {
    return SITE_ROOT_PATH;
  }

}
