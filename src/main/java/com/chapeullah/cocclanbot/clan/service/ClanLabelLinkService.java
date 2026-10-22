package com.chapeullah.cocclanbot.clan.service;

import com.chapeullah.cocclanbot.clan.repository.ClanLabelLinkRepository;
import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.clan.model.ClanLabelLink;
import com.chapeullah.cocclanbot.label.clan.ClanLabel;
import com.chapeullah.cocclanbot.label.clan.ClanLabelService;
import com.chapeullah.cocclanbot.label.dto.LabelResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClanLabelLinkService {

    private final ClanLabelLinkRepository clanLabelLinkRepository;
    private final ClanLabelService clanLabelService;

    @Transactional(readOnly = true)
    public List<ClanLabelLink> getAllByClanTag(@NonNull String clanTag) {
        return clanLabelLinkRepository.findAllByClanTag(clanTag);
    }

    @Transactional
    public List<ClanLabelLink> syncAndGetByClan(
            @NonNull Clan clan,
            List<LabelResponse> responses) {
        List<ClanLabelLink> current = clan.getLabelLinks();
        if (responses == null) {
            log.debug("Clan labels are missing in response; keeping existing label links: clanTag=\"{}\"",
                    clan.getTag());
            return new ArrayList<>(current);
        }
        List<Integer> incomingIds = responses.stream()
                .map(LabelResponse::id)
                .distinct()
                .toList();
        List<ClanLabelLink> incoming = new ArrayList<>();
        for (Integer labelId : incomingIds) {
            ClanLabelLink link = current.stream()
                    .filter(existing -> existing.getLabel().getId().equals(labelId))
                    .findFirst()
                    .orElse(null);
            if (link == null) {
                ClanLabel label = clanLabelService.findById(labelId)
                        .orElseThrow(() -> new IllegalStateException("Clan label not found: labelId=" + labelId));
                link = new ClanLabelLink(clan, label);
            }
            incoming.add(link);
        }
        List<ClanLabelLink> added = incoming.stream()
                .filter(link -> !current.contains(link))
                .toList();
        current.retainAll(incoming);
        current.addAll(clanLabelLinkRepository.saveAll(added));
        return new ArrayList<>(current);
    }

}
