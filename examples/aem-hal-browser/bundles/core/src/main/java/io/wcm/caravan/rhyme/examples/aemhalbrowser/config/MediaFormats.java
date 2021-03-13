package io.wcm.caravan.rhyme.examples.aemhalbrowser.config;

import static io.wcm.handler.media.format.MediaFormatBuilder.create;

import io.wcm.handler.media.format.MediaFormat;

/**
 * Media formats
 */
public final class MediaFormats {

  private MediaFormats() {
    // constants only
  }

  private static final String[] IMAGE_FILE_EXTENSIONS = new String[] {
      "gif", "jpg", "png", "tif", "svg" };

  /**
   * Square
   */
  public static final MediaFormat SQUARE = create("square")
      .label("Square")
      .ratio(1, 1)
      .extensions(IMAGE_FILE_EXTENSIONS)
      .build();

  /**
   * Landscape
   */
  public static final MediaFormat LANDSCAPE = create("landscape")
      .label("Landscape")
      .ratio(16, 9)
      .extensions(IMAGE_FILE_EXTENSIONS)
      .build();

  /**
   * Wide
   */
  public static final MediaFormat WIDE = create("wide")
      .label("Wide")
      .ratio(2, 1)
      .extensions(IMAGE_FILE_EXTENSIONS)
      .build();

  /**
   * Extra Wide
   */
  public static final MediaFormat EXTRA_WIDE = create("extra_wide")
      .label("Extra Wide")
      .ratio(32, 10)
      .extensions(IMAGE_FILE_EXTENSIONS)
      .build();

  /**
   * Portrait
   */
  public static final MediaFormat PORTRAIT = create("portrait")
      .label("Portrait")
      .ratio(1, 2)
      .extensions(IMAGE_FILE_EXTENSIONS)
      .build();

  /**
   * Download
   */
  public static final MediaFormat DOWNLOAD = create("download")
      .label("Download")
      .extensions("pdf", "zip", "ppt", "pptx", "doc", "docx", "jpg", "tif")
      .download(true)
      .build();

}
