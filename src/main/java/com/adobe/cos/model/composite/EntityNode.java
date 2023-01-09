package com.adobe.cos.model.composite;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

@Data
@Node("Entity")
public class EntityNode {
    @Id
    private final String id;

    @Property("asset_id")
    private final String assetId;

    @Property("version")
    private final String version;

//    @Property("fract_index")
//    private final String fractIndex;
//
//    @Relationship(type = "CHILD_OF", direction = INCOMING)
//    private Set<EntityNode> children = new HashSet<>();
//
//    @Relationship(type = "ATTACHED_TO", direction = INCOMING)
//    private Set<ComponentNode> components = new HashSet<>();
}
