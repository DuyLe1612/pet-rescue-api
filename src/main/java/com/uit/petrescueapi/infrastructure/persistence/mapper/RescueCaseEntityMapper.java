package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseJpaEntity;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RescueCaseEntityMapper {

    GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "latitude", source = "location", qualifiedByName = "pointToLat")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "pointToLng")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    RescueCase toDomain(RescueCaseJpaEntity entity);

    @Mapping(target = "location", source = ".", qualifiedByName = "latLngToPoint")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "priorityToString")
    @Mapping(target = "caseType", expression = "java(domain.typeOrRescue().name())")
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "pet", ignore = true)
    RescueCaseJpaEntity toEntity(RescueCase domain);

    List<RescueCase> toDomainList(List<RescueCaseJpaEntity> entities);

    @Named("pointToLat")
    default Double pointToLat(Point point) {
        return point == null ? null : point.getY();
    }

    @Named("pointToLng")
    default Double pointToLng(Point point) {
        return point == null ? null : point.getX();
    }

    @Named("latLngToPoint")
    default Point latLngToPoint(RescueCase rc) {
        if (rc.getLatitude() == null || rc.getLongitude() == null) return null;
        return GF.createPoint(new Coordinate(rc.getLongitude(), rc.getLatitude()));
    }

    @Named("stringToStatus")
    default RescueCaseStatus stringToStatus(String s) {
        return s == null ? null : RescueCaseStatus.valueOf(s);
    }

    @Named("statusToString")
    default String statusToString(RescueCaseStatus s) {
        return s == null ? null : s.name();
    }

    @Named("stringToPriority")
    default RescuePriority stringToPriority(String s) {
        return s == null ? null : RescuePriority.valueOf(s);
    }

    @Named("priorityToString")
    default String priorityToString(RescuePriority p) {
        return p == null ? null : p.name();
    }
}
