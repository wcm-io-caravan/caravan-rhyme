package io.wcm.caravan.rhyme.examples.aemhalbrowser.components;

import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

/**
 * Model that provides the current date.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class CurrentDate {

  private int year;

  @PostConstruct
  private void activate() {
    year = Calendar.getInstance().get(Calendar.YEAR);
  }

  /**
   * @return Current year
   */
  public int getYear() {
    return year;
  }

}
