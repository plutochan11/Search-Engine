package hk.ust.csit5930.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Relationship POJO that represents the parent-child relationship between two pages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Relationship {
    private int id;
    private int parentId;
    private int childId;
}
