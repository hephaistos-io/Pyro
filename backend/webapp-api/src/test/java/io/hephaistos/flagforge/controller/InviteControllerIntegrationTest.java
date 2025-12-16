package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.controller.dto.TeamResponse;
import io.hephaistos.flagforge.data.CustomerRole;
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
}
