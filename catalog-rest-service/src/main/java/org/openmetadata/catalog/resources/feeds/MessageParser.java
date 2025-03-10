/*
 *  Copyright 2021 Collate
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.catalog.resources.feeds;

import static org.openmetadata.catalog.exception.CatalogExceptionMessage.invalidEntityLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class MessageParser {

  private MessageParser() {}

  // Pattern to match the following markdown entity links:
  // <#E/{entityType}/{entityFQN}>  -- <#E/table/bigquery_gcp.shopify.raw_product_catalog>
  // <#E/{entityType}/{entityFQN}/{fieldName}> -- <#E/table/bigquery_gcp.shopify.raw_product_catalog/description>
  // <#E/{entityType}/{entityFQN}/{fieldName}/{arrayFieldName}>
  // -- <#E/table/bigquery_gcp.shopify.raw_product_catalog/columns/comment>
  // <#E/{entityType}/{entityFQN}/{fieldName}/{arrayFieldName}/{arrayFieldValue}>
  // -- <#E/table/bigquery_gcp.shopify.raw_product_catalog/columns/comment/description>
  private static final Pattern ENTITY_LINK_PATTERN =
      Pattern.compile(
          "<#E/"
              + // Match initial string <#E/
              "([^<>]+?)"
              + // Non-greedy collection group 1 for {entityType}
              "/([^<>]+?)"
              + // Non-greedy collection group 2 for {entityFQN}
              "(/([^<>]+?))?"
              + // Non-greedy collection group 3 for optional /{fieldName} and 4 for fieldName
              "(/([^<>]+?))?"
              + // Non-greedy collection group 5 for optional /{arrayFieldName} // and 6 for arrayFieldName
              "(/([^<>]+?))?"
              + // Non-greedy collection group 7 for optional /{arrayFieldValue} // and 8 for arrayFieldValue
              ">"); // Match for end of link name

  public static class EntityLink {
    private final LinkType linkType;
    private final String entityType;
    private final String entityFQN;
    private final String fieldName;
    private final String arrayFieldName;
    private final String arrayFieldValue;
    private final String fullyQualifiedFieldType;
    private final String fullyQualifiedFieldValue;

    public enum LinkType {
      ENTITY,
      ENTITY_REGULAR_FIELD,
      ENTITY_ARRAY_FIELD
    }

    public EntityLink(
        String entityType, String entityFqn, String fieldName, String arrayFieldName, String arrayFieldValue) {
      if (entityType == null || entityFqn == null) {
        throw new IllegalArgumentException("Entity link must have both {entityType} and {entityFQN}");
      }
      this.entityType = entityType;
      this.entityFQN = entityFqn;
      this.fieldName = fieldName;
      this.arrayFieldName = arrayFieldName;
      this.arrayFieldValue = arrayFieldValue;

      if (arrayFieldValue != null) {
        if (arrayFieldName == null) {
          throw new IllegalArgumentException(invalidEntityLink());
        }
        this.linkType = LinkType.ENTITY_ARRAY_FIELD;
        this.fullyQualifiedFieldType = String.format("%s.%s.member", entityType, fieldName);
        this.fullyQualifiedFieldValue = String.format("%s.%s.%s", entityFqn, arrayFieldName, arrayFieldValue);
      } else if (arrayFieldName != null) {
        this.linkType = LinkType.ENTITY_ARRAY_FIELD;
        this.fullyQualifiedFieldType = String.format("%s.%s.member", entityType, fieldName);
        this.fullyQualifiedFieldValue = String.format("%s.%s", entityFqn, arrayFieldName);
      } else if (fieldName != null) {
        this.fullyQualifiedFieldType = String.format("%s.%s", entityType, fieldName);
        this.fullyQualifiedFieldValue = String.format("%s.%s", entityFqn, fieldName);

        this.linkType = LinkType.ENTITY_REGULAR_FIELD;
      } else {
        this.fullyQualifiedFieldType = entityType;
        this.fullyQualifiedFieldValue = entityFqn;
        this.linkType = LinkType.ENTITY;
      }
    }

    public String getLinkString() {
      StringBuilder builder = new StringBuilder();
      builder.append("<#E/").append(entityType).append("/").append(entityFQN);
      if (linkType == LinkType.ENTITY_REGULAR_FIELD || linkType == LinkType.ENTITY_ARRAY_FIELD) {
        builder.append("/").append(fieldName);
      }
      if (linkType == LinkType.ENTITY_ARRAY_FIELD) {
        builder.append("/").append(arrayFieldName);
        if (StringUtils.isNotEmpty(arrayFieldValue)) {
          builder.append("/").append(arrayFieldValue);
        }
      }
      builder.append(">");
      return builder.toString();
    }

    public static EntityLink parse(String link) {
      // Entity links also have support for fallback texts with "|"
      // example: <#E/user/user1|[@User One](http://localhost:8585/user/user1)>
      // Extract the entity link alone if the string has a fallback text
      if (link.contains("|")) {
        link = link.substring(0, link.indexOf("|")) + ">";
      }
      Matcher matcher = ENTITY_LINK_PATTERN.matcher(link);
      EntityLink entityLink = null;
      while (matcher.find()) {
        if (entityLink == null) {
          entityLink =
              new EntityLink(matcher.group(1), matcher.group(2), matcher.group(4), matcher.group(6), matcher.group(8));
        } else {
          throw new IllegalArgumentException("Unexpected multiple entity links in " + link);
        }
      }
      if (entityLink == null) {
        throw new IllegalArgumentException("Entity link was not found in " + link);
      }
      return entityLink;
    }

    public LinkType getLinkType() {
      return linkType;
    }

    public String getEntityType() {
      return entityType;
    }

    public String getEntityFQN() {
      return entityFQN;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getArrayFieldName() {
      return arrayFieldName;
    }

    public String getArrayFieldValue() {
      return arrayFieldValue;
    }

    public String getFullyQualifiedFieldType() {
      return fullyQualifiedFieldType;
    }

    public String getFullyQualifiedFieldValue() {
      return fullyQualifiedFieldValue;
    }

    @Override
    public String toString() {
      return String.format(
          "EntityLink { type = %s, entityType = %s, entityFQN = %s, fieldName = %s, arrayFieldName = %s, arrayFieldValue = %s}",
          linkType, entityType, entityType, fieldName, arrayFieldName, arrayFieldValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EntityLink that = (EntityLink) o;
      return linkType == that.linkType
          && Objects.equals(entityType, that.entityType)
          && Objects.equals(entityFQN, that.entityFQN)
          && Objects.equals(fieldName, that.fieldName)
          && Objects.equals(arrayFieldName, that.arrayFieldName)
          && Objects.equals(arrayFieldValue, that.arrayFieldValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(linkType, entityType, entityFQN, fieldName, arrayFieldName, arrayFieldValue);
    }
  }

  /** Parse the message and get the mentions */
  public static List<EntityLink> getEntityLinks(String message) {
    List<EntityLink> links = new ArrayList<>();
    Matcher matcher = ENTITY_LINK_PATTERN.matcher(message);
    while (matcher.find()) {
      links.add(EntityLink.parse(matcher.group()));
    }
    return links;
  }
}
