package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.member.dto.MemberHouseElementResponse;
import com.chapeullah.guccigoblin.member.dto.MemberHouseElementResponses;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.member.model.MemberHouseElement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberHouseElementService {

    private final MemberHouseElementRepository memberHouseElementRepository;

    @Transactional
    public List<MemberHouseElement> syncAndGetByMember(
            @NonNull Member member,
            MemberHouseElementResponses response) {
        List<MemberHouseElement> current =
                member.getMemberHouseElements();
        Map<Integer, String> currentElementsById = current.stream()
                .collect(Collectors.toMap(
                        MemberHouseElement::getElementId,
                        MemberHouseElement::getElementType));
        Map<Integer, String> incomingElementsById =
                response == null || response.elements() == null
                        ? Map.of()
                        : response.elements().stream()
                        .collect(Collectors.toMap(
                                MemberHouseElementResponse::id,
                                MemberHouseElementResponse::type));
        if (Objects.equals(currentElementsById, incomingElementsById)) {
            return new ArrayList<>(current);
        }
        boolean removed = current.removeIf(element ->
                !Objects.equals(
                        element.getElementType(),
                        incomingElementsById.get(element.getElementId())));
        if (removed) {
            memberHouseElementRepository.flush();
        }
        List<MemberHouseElement> added = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : incomingElementsById.entrySet()) {
            Integer elementId = entry.getKey();
            String elementType = entry.getValue();
            boolean exists = current.stream()
                    .anyMatch(element ->
                            element.getElementId().equals(elementId));
            if (!exists) {
                added.add(new MemberHouseElement(
                        member,
                        elementId,
                        elementType));
            }
        }
        current.addAll(memberHouseElementRepository.saveAll(added));
        return new ArrayList<>(current);
    }
}