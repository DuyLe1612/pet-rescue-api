package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import com.uit.petrescueapi.application.port.command.PetDetailsCommandPort;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.command.RoleCommandPort;
import com.uit.petrescueapi.application.port.command.TagCommandPort;
import com.uit.petrescueapi.application.port.command.UserCommandPort;
import com.uit.petrescueapi.application.port.query.AdoptionQueryPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.application.port.query.PetDetailsQueryPort;
import com.uit.petrescueapi.application.port.query.RescueCaseQueryPort;
import com.uit.petrescueapi.application.port.query.RoleQueryPort;
import com.uit.petrescueapi.application.port.query.TagQueryPort;
import com.uit.petrescueapi.application.port.query.UserQueryPort;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.ReclaimLogRepository;
import com.uit.petrescueapi.infrastructure.security.CustomUserDetailsService;
import com.uit.petrescueapi.infrastructure.security.JwtAccessDeniedHandler;
import com.uit.petrescueapi.infrastructure.security.JwtAuthenticationEntryPoint;
import com.uit.petrescueapi.infrastructure.security.JwtAuthenticationFilter;
import com.uit.petrescueapi.infrastructure.security.JwtService;
import com.uit.petrescueapi.presentation.controller.AdoptionController;
import com.uit.petrescueapi.presentation.controller.PetDetailsController;
import com.uit.petrescueapi.presentation.controller.RescueCaseController;
import com.uit.petrescueapi.presentation.controller.RoleController;
import com.uit.petrescueapi.presentation.controller.TagController;
import com.uit.petrescueapi.presentation.controller.UserController;
import com.uit.petrescueapi.presentation.mapper.AdoptionWebMapper;
import com.uit.petrescueapi.presentation.mapper.RescueCaseWebMapper;
import com.uit.petrescueapi.presentation.mapper.RoleWebMapper;
import com.uit.petrescueapi.presentation.mapper.TagWebMapper;
import com.uit.petrescueapi.presentation.mapper.UserWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real filter chain and method security over HTTP. */
@WebMvcTest(controllers = {RescueCaseController.class, AdoptionController.class, PetDetailsController.class,
        RoleController.class, TagController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class})
class SecurityRulesWebTest {

    private static final String CASE_BODY = "{\"species\":\"DOG\",\"latitude\":10.0,\"longitude\":106.0}";

    @Autowired MockMvc mvc;

    @MockitoBean(name = "corsConfigurationSource") CorsConfigurationSource cors;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @MockitoBean RescueCaseCommandPort rescueCommandPort;
    @MockitoBean RescueCaseQueryPort rescueQueryPort;
    @MockitoBean com.uit.petrescueapi.application.port.command.RescueCaseSharePort rescueSharePort;
    @MockitoBean com.uit.petrescueapi.application.port.query.PetQueryPort petQueryPort;
    @MockitoBean RescueCaseWebMapper rescueMapper;
    @MockitoBean com.uit.petrescueapi.presentation.mapper.PetWebMapper petMapper;
    @MockitoBean AdoptionCommandPort adoptionCommandPort;
    @MockitoBean AdoptionQueryPort adoptionQueryPort;
    @MockitoBean AdoptionWebMapper adoptionMapper;
    @MockitoBean ReclaimLogRepository reclaimLogRepository;
    @MockitoBean PetDetailsCommandPort petDetailsCommandPort;
    @MockitoBean PetDetailsQueryPort petDetailsQueryPort;
    @MockitoBean MediaQueryPort mediaQueryPort;
    @MockitoBean RoleCommandPort roleCommandPort;
    @MockitoBean RoleQueryPort roleQueryPort;
    @MockitoBean RoleWebMapper roleMapper;
    @MockitoBean TagCommandPort tagCommandPort;
    @MockitoBean TagQueryPort tagQueryPort;
    @MockitoBean TagWebMapper tagMapper;
    @MockitoBean UserCommandPort userCommandPort;
    @MockitoBean UserQueryPort userQueryPort;
    @MockitoBean UserWebMapper userMapper;

    private final UUID id = UUID.randomUUID();

    // ── Anonymous writes on rescue cases ────────────

    @Test
    void anonymousCannotReportRescueCase() throws Exception {
        mvc.perform(post("/api/v1/rescue-cases").contentType(MediaType.APPLICATION_JSON).content(CASE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotUpdateRescueCase() throws Exception {
        mvc.perform(put("/api/v1/rescue-cases/" + id).contentType(MediaType.APPLICATION_JSON).content(CASE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotChangeRescueCaseStatus() throws Exception {
        mvc.perform(patch("/api/v1/rescue-cases/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotApproveCompletion() throws Exception {
        mvc.perform(post("/api/v1/rescue-cases/completions/" + id + "/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCanStillReadRescueCases() throws Exception {
        when(rescueQueryPort.findMapMarkers(any(), any(), any())).thenReturn(java.util.List.of());
        mvc.perform(get("/api/v1/rescue-cases/map/markers")).andExpect(status().isOk());
    }

    // ── Forbidden from the domain becomes 403 ───────

    @Test
    void forbiddenFromDomainIsReturnedAs403() throws Exception {
        UUID caller = UUID.randomUUID();
        when(rescueCommandPort.approveCompletion(id, caller)).thenThrow(new ForbiddenException("no"));
        mvc.perform(post("/api/v1/rescue-cases/completions/" + id + "/approve")
                        .with(user(caller.toString()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonApplicantGetsForbiddenOnAdoption() throws Exception {
        UUID caller = UUID.randomUUID();
        when(adoptionQueryPort.findById(id, caller)).thenThrow(new ForbiddenException("no"));
        mvc.perform(get("/api/v1/adoptions/" + id).with(user(caller.toString()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadAdoption() throws Exception {
        mvc.perform(get("/api/v1/adoptions/" + id)).andExpect(status().isUnauthorized());
    }

    // ── Pet records: anonymous reaches the rule, gets 403 not 401 ───

    @Test
    void anonymousOnPrivatePetRecordsGets403NotUnauthorized() throws Exception {
        when(petDetailsQueryPort.findMedicalRecords(any(), any(), isNull()))
                .thenThrow(new ForbiddenException("private"));
        mvc.perform(get("/api/v1/pets/" + id + "/medical-records")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousOnOrgPetRecordsSucceeds() throws Exception {
        when(petDetailsQueryPort.findOwnerships(any(), any(), isNull())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/pets/" + id + "/ownerships")).andExpect(status().isOk());
    }

    // ── Admin-only catalog and user list ────────────

    @Test
    void regularUserCannotDeleteRoleOrTagOrListUsers() throws Exception {
        String caller = UUID.randomUUID().toString();
        mvc.perform(delete("/api/v1/roles/1").with(user(caller).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/tags/" + id).with(user(caller).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").with(user(caller).roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteTag() throws Exception {
        mvc.perform(delete("/api/v1/tags/" + id).with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
