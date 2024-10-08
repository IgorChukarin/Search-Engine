package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    boolean existsByPathAndSiteId(String path, Integer siteId);
    List<Page> findAllByPath(String path);
    Integer countBySiteId(Integer siteId);
}
