package searchengine.services.repositoryService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.repositories.LemmaRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class LemmaServiceImpl implements LemmaService{

    private final LemmaRepository lemmaRepository;

    @Override
    public boolean existsByLemmaAndSiteId(String lemma, Integer siteId) {
        return lemmaRepository.existsByLemmaAndSiteId(lemma, siteId);
    }

    @Override
    public Lemma findByLemmaAndSiteId(String lemma, Integer siteId) {
        return lemmaRepository.findByLemmaAndSiteId(lemma, siteId);
    }

    @Override
    public void save(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    @Override
    public long count() {
        return lemmaRepository.count();
    }

    @Override
    public Integer countBySiteId(Integer siteId) {
        return lemmaRepository.countBySiteId(siteId);
    }

    @Override
    public List<Lemma> findAllByLemma(String lemma) {
        return lemmaRepository.findAllByLemma(lemma);
    }
}
