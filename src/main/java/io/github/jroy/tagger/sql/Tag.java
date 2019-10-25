package io.github.jroy.tagger.sql;

import lombok.Data;

@Data
public class Tag {
  private final int id;
  private final String name;
  private final long price;
  private final String text;
}
