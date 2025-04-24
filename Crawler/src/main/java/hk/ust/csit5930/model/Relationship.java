package hk.ust.csit5930.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Relationship {
    private int id;
    private String parentUrl;
    private String childUrl;
}