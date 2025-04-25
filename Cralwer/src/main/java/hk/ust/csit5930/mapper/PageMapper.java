package hk.ust.csit5930.mapper;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import hk.ust.csit5930.model.Page;
import hk.ust.csit5930.model.Relationship;

public interface PageMapper {
    // Page getPageById(int id);
    // Page getPageByUrl(String url);
    List<Page> getAll();
    int insert(Page page);
    int updateById(Page page);

    Page getParent(String childUrl);
    List<Page> getChildren(String parentUrl);
    void insertRelationship(@Param("parentUrl") String parentUrl, @Param("childUrl") String childUrl);
    List<Relationship> getAllRelationships();
    // void insertPlaceHolder(String url);
}
