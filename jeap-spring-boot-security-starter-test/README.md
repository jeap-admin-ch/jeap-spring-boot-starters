# jEAP Spring Boot Security Starter Test

## `@WithJeapAuthenticationToken`

This annotation can be used on test classes or methods in order to insert a `JeapAuthenticationToken` into the Spring `
SecurityContextHolder`.

### Usage example 1

The refund request entity is created with the `EntityFactory.DEFAULT_BP_ID`. The user needs to have the same bpId in
order to retrieve this entity via the API. In the first test, the authenticated user has the same bpId as the refund
request and can therefore retrieve it. In the second test, the user has a different bpId and can not retrieve the refund
request.

```java

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class RefundRequestControllerITest {

    @Autowired
    private MockMvc mockMvc;

    // other autowired beans left out for brevity

    @Test
    @WithJeapAuthenticationToken(userRoles = "camiuns_@refund_#read", bpRoles = EntityFactory.DEFAULT_BP_ID + " = camiuns_@refund_#read")
    void getRefundRequest() throws Exception {
        // Arrange
        final RefundRequest refundRequest = refundRequestRepository.save(EntityFactory.refundRequest());
        final long refundRequestId = refundRequest.getId();

        // Act
        final RefundRequestDto refundRequestDto = objectMapper.readValue(
                mockMvc.perform(get("/api/refund-requests/{id}", refundRequestId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                RefundRequestDto.class);

        // Assert
        verify(refundRequestService).getRefundRequest(refundRequestId, refundRequest.getBpId());
        assertThat(refundRequestDto).usingRecursiveComparison().isEqualTo(refundRequest);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = "camiuns_@refund_#read", bpRoles = "abc123 = camiuns_@refund_#read")
    void getRefundRequest() throws Exception {
        // Arrange
        final RefundRequest refundRequest = refundRequestRepository.save(EntityFactory.refundRequest());
        final long refundRequestId = refundRequest.getId();

        // Act
        mockMvc.perform(get("/api/refund-requests/{id}", refundRequestId))
                .andExpect(status().isNotFound());

        // Assert
        verify(refundRequestService).getRefundRequest(refundRequestId, "abc123");
    }
}
```

### Usage example 2

This test verifies that the authenticated user's extId and username are saved on the entity that is created with this
API call.

```java

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuditControllerITest {

    private static final String EXT_ID = "123456789";
    private static final String USERNAME = "test-user";

    @Autowired
    private MockMvc mockMvc;

    // other autowired beans left out for brevity

    @Test
    @WithJeapAuthenticationToken(displayName = USERNAME, extId = EXT_ID, userRoles = "camiuns_@iks_#write")
    void createAudit() throws Exception {
        // Arrange
        final UUID manualChangeId = manualChangeRepository.save(EntityFactory.manualChange()).getId();
        final AuditResultDto auditResultDto = DtoFactory.audit();

        // Act
        final AuditDto auditDto = objectMapper.readValue(
                mockMvc.perform(post("/api/manual-changes/{manualChangeId}/audits", manualChangeId)
                        .content(objectMapper.writeValueAsString(auditResultDto))
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                AuditDto.class);

        // Assert
        verify(auditService).createAudit(manualChangeId, auditResultDto);
        assertThat(auditResultDto).usingRecursiveComparison().isEqualTo(auditDto);
        transactionTemplate.executeWithoutResult(s -> {
            final Audit audit = auditRepository.findById(auditDto.getId()).orElseThrow();
            assertThat(auditResultDto).usingRecursiveComparison().isEqualTo(audit);
            assertThat(audit.getAuditor().getExtId()).isEqualTo(EXT_ID);
            assertThat(audit.getAuditor().getUsername()).isEqualTo(USERNAME);
        });
    }
}
```

### Usage example 3

This test verifies that the API can only be accessed with a certain role (negative testing).

```java

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AssessmentPeriodSummaryControllerITest {

    @Autowired
    private MockMvc mockMvc;

    // other autowired beans left out for brevity

    @Test
    @WithJeapAuthenticationToken(userRoles = "camiuns_@assessment_#read")
    void createAssessmentPeriodSummaries_wrongRole() throws Exception {
        // Arrange
        final AssessmentPeriodSummaryRequestDto request = DtoFactory.domesticAssessmentPeriodSummaryRequestDto();

        // Act
        mockMvc.perform(post("/api/assessment-period-summary/{period}", 202201)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Assert
        verifyNoInteractions(assessmentPeriodSummaryService);
    }
}
```
