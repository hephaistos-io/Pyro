package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.controller.dto.TeamResponse;
import io.hephaistos.flagforge.data.repository.CompanyInviteRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.InvalidInviteException.InvalidInviteReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestContainerConfiguration.class)
@Tag("integration")
class InviteControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyInviteRepository companyInviteRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        companyInviteRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /v1/company/invite - Create Invite")
    class CreateInvite {

        @Test
        void createInviteReturns201WithValidToken() {
            // Setup: register user, create company, re-authenticate
            String token = registerAndAuthenticateWithCompany();
            token = authenticate(); // Re-authenticate to get company in token

            var request =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);

            var response = post("/v1/company/invite", request, token, InviteCreationResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().token()).hasSize(64);
            assertThat(response.getBody().email()).isEqualTo("newuser@example.com");
            assertThat(response.getBody().role()).isEqualTo(CustomerRole.DEV);
            assertThat(response.getBody().inviteUrl()).contains(response.getBody().token());
        }

        @Test
        void createInviteWithApplicationAccess() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create an application
            var appRequest = new ApplicationCreationRequest("Test App");
            var appResponse =
                    post("/v1/applications", appRequest, token, ApplicationResponse.class);
            assertThat(appResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            var request = new InviteCreationRequest("newuser@example.com", CustomerRole.DEV,
                    Set.of(appResponse.getBody().id()), null);

            var response = post("/v1/company/invite", request, token, InviteCreationResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().applicationIds()).containsExactly(
                    appResponse.getBody().id());
        }

        @Test
        void createInviteRequiresAuthentication() {
            var request =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);

            var response = post("/v1/company/invite", request, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void createInviteRequiresCompany() {
            // Register without creating a company
            String token = registerAndAuthenticate();

            var request =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);

            var response = post("/v1/company/invite", request, token,
                    GlobalExceptionHandler.ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void createInviteRequiresAdminRole() {
            // Setup: admin creates invite for a DEV user
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            var devInviteRequest =
                    new InviteCreationRequest("dev@example.com", CustomerRole.DEV, null, null);
            var devInviteResponse = post("/v1/company/invite", devInviteRequest, adminToken,
                    InviteCreationResponse.class);

            // DEV user registers and authenticates
            registerUserWithInvite("Dev", "User", "password123",
                    devInviteResponse.getBody().token());
            String devToken = authenticate("dev@example.com", "password123");

            // DEV user tries to create an invite - should fail with 403
            var request =
                    new InviteCreationRequest("another@example.com", CustomerRole.READ_ONLY, null,
                            null);

            var response = post("/v1/company/invite", request, devToken,
                    GlobalExceptionHandler.ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        }
    }


    @Nested
    @DisplayName("GET /v1/invite/{token} - Validate Invite")
    class ValidateInvite {

        @Test
        void validateInviteReturnsValidForGoodToken() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Validate (public endpoint, no auth needed)
            var validateResponse = get("/v1/invite/" + createResponse.getBody().token(),
                    InviteValidationResponse.class);

            assertThat(validateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(validateResponse.getBody().valid()).isTrue();
            assertThat(validateResponse.getBody().email()).isEqualTo("newuser@example.com");
            assertThat(validateResponse.getBody().companyName()).isEqualTo("Test Company");
            assertThat(validateResponse.getBody().role()).isEqualTo(CustomerRole.DEV);
        }

        @Test
        void validateInviteReturnsNotFoundForInvalidToken() {
            var response = get("/v1/invite/nonexistenttoken123456", InviteValidationResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().valid()).isFalse();
            assertThat(response.getBody().reason()).isEqualTo(InvalidInviteReason.NOT_FOUND);
        }

        @Test
        void validateInviteReturnsExpiredForExpiredToken() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite with 1 day expiry
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, 1);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Manually expire the invite in the database
            var invite =
                    companyInviteRepository.findByToken(createResponse.getBody().token()).get();
            invite.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
            companyInviteRepository.save(invite);

            var validateResponse = get("/v1/invite/" + createResponse.getBody().token(),
                    InviteValidationResponse.class);

            assertThat(validateResponse.getBody().valid()).isFalse();
            assertThat(validateResponse.getBody().reason()).isEqualTo(InvalidInviteReason.EXPIRED);
        }
    }


    @Nested
    @DisplayName("Register with Invite")
    class RegisterWithInvite {

        @Test
        void registerWithInviteCreatesUserWithCorrectCompanyAndRole() {
            // Admin creates invite
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            var inviteRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var inviteResponse = post("/v1/company/invite", inviteRequest, adminToken,
                    InviteCreationResponse.class);

            // New user registers with invite
            registerUserWithInvite("Jane", "Smith", "password123",
                    inviteResponse.getBody().token());

            // Verify user was created with correct values
            var customer = customerRepository.findByEmail("invited@example.com");
            assertThat(customer).isPresent();
            assertThat(customer.get().getFirstName()).isEqualTo("Jane");
            assertThat(customer.get().getLastName()).isEqualTo("Smith");
            assertThat(customer.get().getRole()).isEqualTo(CustomerRole.DEV);
            assertThat(customer.get().getCompanyId()).isPresent();
        }

        @Test
        void registerWithInviteMarksInviteAsUsed() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            var inviteRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var inviteResponse = post("/v1/company/invite", inviteRequest, adminToken,
                    InviteCreationResponse.class);

            registerUserWithInvite("Jane", "Smith", "password123",
                    inviteResponse.getBody().token());

            // Verify invite is marked as used
            var invite =
                    companyInviteRepository.findByToken(inviteResponse.getBody().token()).get();
            assertThat(invite.getUsedAt()).isNotNull();
            assertThat(invite.getUsedBy()).isNotNull();
        }

        @Test
        void registerWithSameInviteTwiceFails() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            var inviteRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var inviteResponse = post("/v1/company/invite", inviteRequest, adminToken,
                    InviteCreationResponse.class);

            // First registration succeeds
            registerUserWithInvite("Jane", "Smith", "password123",
                    inviteResponse.getBody().token());

            // Second registration with same token fails
            var registration = CustomerRegistrationRequest.withInvite("John", "Doe", "password456",
                    inviteResponse.getBody().token());
            var response = post("/v1/auth/register", registration,
                    GlobalExceptionHandler.ErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo("INVALID_INVITE");
        }

        @Test
        void registerWithInviteAssignsApplicationAccess() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            // Create an application
            var appRequest = new ApplicationCreationRequest("Test App");
            var appResponse =
                    post("/v1/applications", appRequest, adminToken, ApplicationResponse.class);

            // Create invite with app access
            var inviteRequest = new InviteCreationRequest("invited@example.com", CustomerRole.DEV,
                    Set.of(appResponse.getBody().id()), null);
            var inviteResponse = post("/v1/company/invite", inviteRequest, adminToken,
                    InviteCreationResponse.class);

            registerUserWithInvite("Jane", "Smith", "password123",
                    inviteResponse.getBody().token());

            // Verify user has app access
            var customer =
                    customerRepository.findByEmailWithAccessibleApplications("invited@example.com");
            assertThat(customer).isPresent();
            assertThat(customer.get().getAccessibleApplications()).hasSize(1);
            assertThat(
                    customer.get().getAccessibleApplications().iterator().next().getId()).isEqualTo(
                    appResponse.getBody().id());
        }
    }


    @Nested
    @DisplayName("GET /v1/customer/all - Team Response with Pending Invites")
    class TeamWithInvites {

        @Test
        void getTeamReturnsTeamResponseWithMembersAndPendingInvites() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            // Create a pending invite
            var inviteRequest =
                    new InviteCreationRequest("pending@example.com", CustomerRole.DEV, null, null);
            post("/v1/company/invite", inviteRequest, adminToken, InviteCreationResponse.class);

            var response = get("/v1/customer/all", adminToken, TeamResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().members()).hasSize(1); // The admin
            assertThat(response.getBody().pendingInvites()).hasSize(1);
            assertThat(response.getBody().pendingInvites().getFirst().email()).isEqualTo(
                    "pending@example.com");
        }

        @Test
        void pendingInvitesAreFilteredByCompany() {
            // Company A creates invite
            registerUser("Admin", "A", "admin-a@example.com", "password123");
            String tokenA = authenticate("admin-a@example.com", "password123");
            createCompany(tokenA, "Company A");
            tokenA = authenticate("admin-a@example.com", "password123");

            var inviteRequestA =
                    new InviteCreationRequest("invite-a@example.com", CustomerRole.DEV, null, null);
            post("/v1/company/invite", inviteRequestA, tokenA, InviteCreationResponse.class);

            // Company B creates invite
            registerUser("Admin", "B", "admin-b@example.com", "password123");
            String tokenB = authenticate("admin-b@example.com", "password123");
            createCompany(tokenB, "Company B");
            tokenB = authenticate("admin-b@example.com", "password123");

            var inviteRequestB =
                    new InviteCreationRequest("invite-b@example.com", CustomerRole.DEV, null, null);
            post("/v1/company/invite", inviteRequestB, tokenB, InviteCreationResponse.class);

            // Company A should only see their invite
            var responseA = get("/v1/customer/all", tokenA, TeamResponse.class);
            assertThat(responseA.getBody().pendingInvites()).hasSize(1);
            assertThat(responseA.getBody().pendingInvites().getFirst().email()).isEqualTo(
                    "invite-a@example.com");

            // Company B should only see their invite
            var responseB = get("/v1/customer/all", tokenB, TeamResponse.class);
            assertThat(responseB.getBody().pendingInvites()).hasSize(1);
            assertThat(responseB.getBody().pendingInvites().getFirst().email()).isEqualTo(
                    "invite-b@example.com");
        }

        @Test
        void usedInvitesAreNotReturnedAsPending() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();

            // Create and use an invite
            var inviteRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var inviteResponse = post("/v1/company/invite", inviteRequest, adminToken,
                    InviteCreationResponse.class);

            registerUserWithInvite("Jane", "Smith", "password123",
                    inviteResponse.getBody().token());

            // Team should show the new member but not the used invite
            var response = get("/v1/customer/all", adminToken, TeamResponse.class);

            assertThat(response.getBody().members()).hasSize(2); // Admin + invited user
            assertThat(response.getBody().pendingInvites()).isEmpty();
        }
    }


    @Nested
    @DisplayName("POST /v1/company/invite/{id}/regenerate - Regenerate Invite")
    class RegenerateInvite {

        @Test
        void regenerateInviteReturns200WithNewToken() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);
            String originalToken = createResponse.getBody().token();

            // Regenerate invite
            var regenerateResponse =
                    post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate",
                            null, token, InviteCreationResponse.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(regenerateResponse.getBody()).isNotNull();
            assertThat(regenerateResponse.getBody().token()).hasSize(64);
            assertThat(regenerateResponse.getBody().token()).isNotEqualTo(originalToken);
            assertThat(regenerateResponse.getBody().email()).isEqualTo("newuser@example.com");
            assertThat(regenerateResponse.getBody().role()).isEqualTo(CustomerRole.DEV);
        }

        @Test
        void regenerateInviteInvalidatesOldToken() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);
            String originalToken = createResponse.getBody().token();

            // Regenerate invite
            post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate", null, token,
                    InviteCreationResponse.class);

            // Validate old token should fail
            var validateResponse =
                    get("/v1/invite/" + originalToken, InviteValidationResponse.class);
            assertThat(validateResponse.getBody().valid()).isFalse();
            assertThat(validateResponse.getBody().reason()).isEqualTo(
                    InvalidInviteReason.NOT_FOUND);
        }

        @Test
        void regenerateInviteNewTokenIsValid() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Regenerate invite
            var regenerateResponse =
                    post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate",
                            null, token, InviteCreationResponse.class);

            // Validate new token should succeed
            var validateResponse = get("/v1/invite/" + regenerateResponse.getBody().token(),
                    InviteValidationResponse.class);
            assertThat(validateResponse.getBody().valid()).isTrue();
            assertThat(validateResponse.getBody().email()).isEqualTo("newuser@example.com");
        }

        @Test
        void regenerateInvitePreservesEmailRoleAndApplications() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create an application
            var appRequest = new ApplicationCreationRequest("Test App");
            var appResponse =
                    post("/v1/applications", appRequest, token, ApplicationResponse.class);

            // Create invite with role and apps
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.READ_ONLY,
                            Set.of(appResponse.getBody().id()), null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Regenerate invite
            var regenerateResponse =
                    post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate",
                            null, token, InviteCreationResponse.class);

            assertThat(regenerateResponse.getBody().email()).isEqualTo("newuser@example.com");
            assertThat(regenerateResponse.getBody().role()).isEqualTo(CustomerRole.READ_ONLY);
            assertThat(regenerateResponse.getBody().applicationIds()).containsExactly(
                    appResponse.getBody().id());
        }

        @Test
        void regenerateInviteResetsExpiration() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite with 1 day expiry
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, 1);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Manually expire the invite
            var invite =
                    companyInviteRepository.findByToken(createResponse.getBody().token()).get();
            invite.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            companyInviteRepository.save(invite);

            // Regenerate invite (should reset to 7 days)
            var regenerateResponse =
                    post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate",
                            null, token, InviteCreationResponse.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            // New expiration should be roughly 7 days from now
            assertThat(regenerateResponse.getBody().expiresAt()).isAfter(
                    Instant.now().plus(6, ChronoUnit.DAYS));
        }

        @Test
        void regenerateUsedInviteReturns403() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create and use an invite
            var createRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            registerUserWithInvite("Jane", "Smith", "password123",
                    createResponse.getBody().token());

            // Try to regenerate used invite
            var regenerateResponse =
                    post("/v1/company/invite/" + createResponse.getBody().id() + "/regenerate",
                            null, token, GlobalExceptionHandler.ErrorResponse.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void regenerateNonExistentInviteReturns404() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            var regenerateResponse =
                    post("/v1/company/invite/" + UUID.randomUUID() + "/regenerate", null, token,
                            GlobalExceptionHandler.ErrorResponse.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void regenerateInviteFromAnotherCompanyReturns404() {
            // Company A creates invite
            registerUser("Admin", "A", "admin-a@example.com", "password123");
            String tokenA = authenticate("admin-a@example.com", "password123");
            createCompany(tokenA, "Company A");
            tokenA = authenticate("admin-a@example.com", "password123");

            var inviteRequestA =
                    new InviteCreationRequest("invite-a@example.com", CustomerRole.DEV, null, null);
            var inviteResponseA = post("/v1/company/invite", inviteRequestA, tokenA,
                    InviteCreationResponse.class);

            // Company B tries to regenerate Company A's invite
            registerUser("Admin", "B", "admin-b@example.com", "password123");
            String tokenB = authenticate("admin-b@example.com", "password123");
            createCompany(tokenB, "Company B");
            tokenB = authenticate("admin-b@example.com", "password123");

            var regenerateResponse =
                    post("/v1/company/invite/" + inviteResponseA.getBody().id() + "/regenerate",
                            null, tokenB, GlobalExceptionHandler.ErrorResponse.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void regenerateInviteRequiresAuthentication() {
            var regenerateResponse =
                    post("/v1/company/invite/" + UUID.randomUUID() + "/regenerate", null,
                            Void.class);

            assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }


    @Nested
    @DisplayName("DELETE /v1/company/invite/{id} - Delete Invite")
    class DeleteInvite {

        @Test
        void deleteInviteReturns204() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Delete invite
            var deleteResponse =
                    delete("/v1/company/invite/" + createResponse.getBody().id(), token,
                            Void.class);

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void deleteInviteInvalidatesToken() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);
            String inviteToken = createResponse.getBody().token();

            // Delete invite
            delete("/v1/company/invite/" + createResponse.getBody().id(), token, Void.class);

            // Validate token should fail
            var validateResponse = get("/v1/invite/" + inviteToken, InviteValidationResponse.class);
            assertThat(validateResponse.getBody().valid()).isFalse();
            assertThat(validateResponse.getBody().reason()).isEqualTo(
                    InvalidInviteReason.NOT_FOUND);
        }

        @Test
        void deleteInviteRemovesFromPendingList() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create invite
            var createRequest =
                    new InviteCreationRequest("newuser@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            // Verify it's in pending list
            var teamBefore = get("/v1/customer/all", token, TeamResponse.class);
            assertThat(teamBefore.getBody().pendingInvites()).hasSize(1);

            // Delete invite
            delete("/v1/company/invite/" + createResponse.getBody().id(), token, Void.class);

            // Verify it's no longer in pending list
            var teamAfter = get("/v1/customer/all", token, TeamResponse.class);
            assertThat(teamAfter.getBody().pendingInvites()).isEmpty();
        }

        @Test
        void deleteNonExistentInviteReturns404() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            var deleteResponse = delete("/v1/company/invite/" + UUID.randomUUID(), token,
                    GlobalExceptionHandler.ErrorResponse.class);

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void deleteInviteFromAnotherCompanyReturns404() {
            // Company A creates invite
            registerUser("Admin", "A", "admin-a@example.com", "password123");
            String tokenA = authenticate("admin-a@example.com", "password123");
            createCompany(tokenA, "Company A");
            tokenA = authenticate("admin-a@example.com", "password123");

            var inviteRequestA =
                    new InviteCreationRequest("invite-a@example.com", CustomerRole.DEV, null, null);
            var inviteResponseA = post("/v1/company/invite", inviteRequestA, tokenA,
                    InviteCreationResponse.class);

            // Company B tries to delete Company A's invite
            registerUser("Admin", "B", "admin-b@example.com", "password123");
            String tokenB = authenticate("admin-b@example.com", "password123");
            createCompany(tokenB, "Company B");
            tokenB = authenticate("admin-b@example.com", "password123");

            var deleteResponse =
                    delete("/v1/company/invite/" + inviteResponseA.getBody().id(), tokenB,
                            GlobalExceptionHandler.ErrorResponse.class);

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void deleteInviteRequiresAuthentication() {
            var deleteResponse =
                    delete("/v1/company/invite/" + UUID.randomUUID(), null, Void.class);

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void deleteUsedInviteStillWorks() {
            String token = registerAndAuthenticateWithCompany();
            token = authenticate();

            // Create and use an invite
            var createRequest =
                    new InviteCreationRequest("invited@example.com", CustomerRole.DEV, null, null);
            var createResponse =
                    post("/v1/company/invite", createRequest, token, InviteCreationResponse.class);

            registerUserWithInvite("Jane", "Smith", "password123",
                    createResponse.getBody().token());

            // Delete used invite - should still work (cleanup)
            var deleteResponse =
                    delete("/v1/company/invite/" + createResponse.getBody().id(), token,
                            Void.class);

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
