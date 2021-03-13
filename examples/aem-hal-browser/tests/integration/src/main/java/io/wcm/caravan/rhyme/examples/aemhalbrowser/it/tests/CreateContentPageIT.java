package io.wcm.caravan.rhyme.examples.aemhalbrowser.it.tests;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.it.rules.Templates.CONTENTPAGE_TEMPLATE_PATH;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ComponentClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.assertion.CQAssert;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;

import io.wcm.caravan.rhyme.examples.aemhalbrowser.it.components.Title;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.it.rules.SiteRule;

/**
 * Sample integration test that creates a content page with a component on it on author and publish.
 */
public class CreateContentPageIT {

  private static final Logger log = LoggerFactory.getLogger(CreateContentPageIT.class);

  /**
   * Represents author and publish service. Hostname and port are read from system properties.
   */
  @ClassRule
  public static final CQAuthorPublishClassRule CQ_BASE_CLASS_RULE = new CQAuthorPublishClassRule();

  /**
   * Decorates the test and adds additional functionality on top of it, like session stickyness,
   * test filtering and identification of the test on the remote service.
   */
  @Rule
  public CQRule cqBaseRule = new CQRule(CQ_BASE_CLASS_RULE.authorRule, CQ_BASE_CLASS_RULE.publishRule);

  private static CQClient adminAuthor;
  private static CQClient adminPublish;

  /**
   * Create two CQClient instances bound to the admin user on both the author and publish service.
   */
  @BeforeClass
  @SuppressWarnings("null")
  public static void beforeClass() {
    adminAuthor = CQ_BASE_CLASS_RULE.authorRule.getAdminClient(CQClient.class);
    adminPublish = CQ_BASE_CLASS_RULE.publishRule.getAdminClient(CQClient.class);
  }


  /**
   * This rules creates a site with a root page using the project's homepage template.
   */
  @Rule
  public SiteRule site = new SiteRule(CQ_BASE_CLASS_RULE.authorRule);

  /**
   * Create a content page with a component.
   */
  @Test
  @SuppressWarnings({
      "null",
      "AEM Rules:AEM-2" // no access to com.day.cq.commons.jcr.JcrConstants in integration tests
  })
  public void testCreateContentPage() throws ClientException, InterruptedException, IOException {

    log.info("Create content page below {}", site.getRootPath());
    String contentPagePath;
    try (SlingHttpResponse response = adminAuthor.createPage("my-content", "My Content", site.getRootPath(),
        CONTENTPAGE_TEMPLATE_PATH)) {
      contentPagePath = response.getSlingPath();
    }

    log.info("Create title component in {}", contentPagePath);
    ComponentClient componentClient = adminAuthor.adaptTo(ComponentClient.class);
    componentClient.setDefaultComponentRelativeLocation("/jcr:content/content/*");
    Title title = componentClient.addComponent(Title.class, contentPagePath);

    log.info("Set custom title for {}", title.getComponentPath());
    String titleString = "Current time: " + DateFormat.getDateTimeInstance().format(new Date());
    title.setProperty("jcr:title", titleString);
    title.save();

    log.info("Activate page {}", contentPagePath);
    ReplicationClient replicationClient = adminAuthor.adaptTo(ReplicationClient.class);
    replicationClient.activate(contentPagePath);

    log.info("Assert page on publish {}", contentPagePath);
    CQAssert.assertCQPageExistsWithTimeout(adminPublish, contentPagePath, 20000, 1000);

    log.info("Assert custom title is set on publish {}", contentPagePath);
    String url = adminPublish.getUrl(contentPagePath + "/.html").toString();
    try (SlingHttpResponse response = adminPublish.doGet(url, 200)) {
      response.checkContentContains(titleString);
    }
  }

}
