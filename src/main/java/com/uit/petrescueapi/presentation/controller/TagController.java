package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.tag.*;
import com.uit.petrescueapi.application.port.command.TagCommandPort;
import com.uit.petrescueapi.application.port.query.TagQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.TagWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Tag management")
public class TagController {

    private final TagCommandPort commandPort;
    private final TagQueryPort queryPort;
    private final TagWebMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new tag")
    public ResponseEntity<ApiResponse<TagResponseDto>> create(
            @Valid @RequestBody CreateTagRequestDto cmd) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.create(cmd))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        commandPort.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping
    @Operation(summary = "List all tags")
    public ResponseEntity<ApiResponse<PageResponse<TagSummaryResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }
}
