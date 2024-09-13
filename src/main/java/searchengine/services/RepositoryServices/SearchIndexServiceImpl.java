package searchengine.services.RepositoryServices;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.SearchIndex;
import searchengine.repositories.SearchIndexRepository;

@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService{
    private final SearchIndexRepository searchIndexRepository;

    @Override
    public void save(SearchIndex searchIndex) {
        searchIndexRepository.save(searchIndex);
    }
}
