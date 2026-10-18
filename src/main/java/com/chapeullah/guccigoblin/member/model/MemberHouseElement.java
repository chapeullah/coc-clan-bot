package com.chapeullah.guccigoblin.member.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(name = "member_house_elements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_house_elements_member_id_element_id",
                columnNames = {"member_id", "element_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberHouseElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "element_id", nullable = false)
    private Integer elementId;

    @Column(name = "element_type", nullable = false)
    private String elementType;

    public MemberHouseElement(
            @NonNull Member member,
            @NonNull Integer elementId,
            @NonNull String elementType) {
        this.member = member;
        this.elementId = elementId;
        this.elementType = elementType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MemberHouseElement other)) {
            return false;
        }
        return Objects.equals(this.getMember(), other.getMember())
                && Objects.equals(this.getElementId(), other.getElementId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getMember(), this.getElementId());
    }
}
