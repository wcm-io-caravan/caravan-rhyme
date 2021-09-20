package io.wcm.caravan.rhyme.spring.impl;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

@RestController
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class SpringRhymeDocsIntegration implements RhymeDocsSupport {

  private static final String PATH = "/docs/rhyme/api/";

  @GetMapping(path = PATH + "{fileName}")
  public String getHtml(@PathVariable("fileName") String fileName) {

    return RhymeDocsSupport.loadGeneratedHtml(this, fileName);
  }

  @Override
  public String getRhymeDocsBaseUrl() {

    return SpringRhymeDocsIntegration.PATH;
  }

  @Override
  public InputStream openResourceStream(String resourcePath) throws IOException {

    Resource resource = new ClassPathResource(resourcePath);

    return resource.getInputStream();
  }

  @Override
  public boolean isFragmentAppendedToCuriesLink() {

    return true;
  }

}
