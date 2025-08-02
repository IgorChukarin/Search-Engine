package searchengine.services.repositoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.SearchIndex;
import searchengine.repository.SearchIndexRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService{
    private final SearchIndexRepository searchIndexRepository;

    @Override
    public void save(SearchIndex searchIndex) {
        searchIndexRepository.save(searchIndex);
    }

    @Override
    public void saveAll(List<SearchIndex> searchIndices) {
        searchIndexRepository.saveAll(searchIndices);
    }
}
