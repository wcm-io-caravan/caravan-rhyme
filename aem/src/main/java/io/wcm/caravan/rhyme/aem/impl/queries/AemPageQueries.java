package io.wcm.caravan.rhyme.aem.impl.queries;

import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

/**
 * A sling model to quickly find page content or attributes with JCR queries.
 * There will only be a single cached instance of this model for each request, and repeated calls for the same root path
 * will return a result immediately.
 */
@Model(adaptables = SlingHttpServletRequest.class, cache = true)
public class AemPageQueries {

  private final LoadingCache<String, Instant> lastModifiedCache = CacheBuilder.newBuilder().build(new LastModifiedCacheLoader());

  @Inject
  private ResourceResolver resolver;

  /**
   * Finds the latest modification time of any cq:Page below the given root path.
   * @param rootPath from which pages should be considered
   * @return the latest "cq:lastModified" time from all "cq:PageContent" resources beneath
   */
  @SuppressWarnings("PMD.PreserveStackTrace")
  public Instant getLastModifiedDateBelow(String rootPath) {

    try {
      return lastModifiedCache.get(rootPath);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new HalApiDeveloperException("Failed to get most recent cq:lastModified below " + rootPath, ex.getCause());
    }
  }

  private class LastModifiedCacheLoader extends CacheLoader<String, Instant> {

    @Override
    public Instant load(String rootPath) throws Exception {

      Session session = resolver.adaptTo(Session.class);
      if (session == null) {
        throw new HalApiDeveloperException("Could not adapt ResourceResolver to JCR Session");
      }
      QueryManager queryManager = session.getWorkspace().getQueryManager();

      String xpath = getLastModifiedPageContentQuery(rootPath);
      Query query = queryManager.createQuery(xpath, "xpath");

      QueryResult result = query.execute();
      RowIterator rows = result.getRows();
      if (!rows.hasNext()) {
        throw new HalApiDeveloperException("Not a single cq:PageContent node was found below " + rootPath);
      }

      Row firstRow = rows.nextRow();
      Calendar lastModified = firstRow.getValue("cq:lastModified").getDate();

      return Instant.ofEpochMilli(lastModified.getTimeInMillis());
    }
  }

  public static String getLastModifiedPageContentQuery(String rootPath) {
    return "/jcr:root" + rootPath + "//element(*, cq:PageContent) order by @cq:lastModified descending";
  }
}

