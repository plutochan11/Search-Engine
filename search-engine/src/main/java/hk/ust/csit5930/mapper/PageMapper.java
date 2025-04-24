package hk.ust.csit5930.mapper;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import hk.ust.csit5930.model.Page;

public interface PageMapper {
    // Page getPageById(int id);
    // Page getPageByUrl(String url);
    List<Page> getAll();
    Page getParent(String childUrl);
    List<Page> getChildren(String parentUrl);
    int insert(Page page);
    int updateById(Page page);
    void insertRelationship(@Param("parentUrl") String parentUrl, @Param("childUrl") String childUrl);
    void insertPlaceHolder(String url);
}
