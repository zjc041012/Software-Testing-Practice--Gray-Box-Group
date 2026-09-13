package com.example.waybill.service;

import com.example.waybill.entity.EncryptedFile;
import com.example.waybill.entity.Role;
import com.example.waybill.entity.User;
import com.example.waybill.entity.WaybillRecord;
import com.example.waybill.repository.EncryptedFileRepository;
import com.example.waybill.repository.FileKeyGrantRepository;
import com.example.waybill.repository.WaybillRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private WaybillRecordRepository waybillRepository;
    @Mock
    private EncryptedFileRepository fileRepository;
    @Mock
    private FileKeyGrantRepository grantRepository;

    private AccessControlService accessControlService;
    private WaybillRecord waybill;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(currentUserService, waybillRepository, fileRepository, grantRepository);
        waybill = new WaybillRecord();
        waybill.setWaybillId("WB-001");
        waybill.setConsignorOrgId(10L);
        waybill.setForwarderOrgId(20L);
        waybill.setCarrierOrgId(30L);
    }

    @Test
    void adminCanCreateAnyWaybill() {
        assertThat(accessControlService.isAdmin(user(Role.ADMIN, null))).isTrue();

        accessControlService.requireCanCreateWaybill(user(Role.ADMIN, null), waybill);
    }

    @Test
    void consignorCanCreateAWaybillForItsOwnOrganization() {
        accessControlService.requireCanCreateWaybill(user(Role.CONSIGNOR, 10L), waybill);
    }

    @Test
    void consignorCannotCreateAWaybillForAnotherOrganization() {
        assertThatThrownBy(() -> accessControlService.requireCanCreateWaybill(user(Role.CONSIGNOR, 99L), waybill))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crossChainGatewayCannotSeeOrdinaryWaybills() {
        User gateway = user(Role.CROSS_CHAIN_GATEWAY, 1L);

        assertThat(accessControlService.filterVisibleWaybills(gateway, List.of(waybill))).isEmpty();
    }

    @Test
    void uploaderCanDownloadItsOwnFile() {
        User uploader = user(Role.CONSIGNOR, 10L);
        uploader.setId(7L);
        EncryptedFile file = new EncryptedFile();
        file.setUploaderId(7L);

        assertThat(accessControlService.canDownload(uploader, file)).isTrue();
    }

    @Test
    void aNonUploaderWithoutAnActiveGrantCannotDownload() {
        User other = user(Role.CONSIGNEE, 40L);
        other.setId(8L);
        EncryptedFile file = new EncryptedFile();
        file.setId(12L);
        file.setUploaderId(7L);
        when(grantRepository.existsByFileIdAndGrantStatusAndGranteeUserId(anyLong(), any(), anyLong())).thenReturn(false);
        when(grantRepository.existsByFileIdAndGrantStatusAndGranteeOrgId(anyLong(), any(), anyLong())).thenReturn(false);
        when(grantRepository.existsByFileIdAndGrantStatusAndGranteeRole(anyLong(), any(), any())).thenReturn(false);

        assertThat(accessControlService.canDownload(other, file)).isFalse();
    }

    @Test
    void onlyConfiguredRolesCanBeGrantTargets() {
        accessControlService.requireAllowedGrantTarget(Role.CONSIGNEE);
        accessControlService.requireAllowedGrantTarget(Role.CUSTOMS);
        accessControlService.requireAllowedGrantTarget(Role.FORWARDER);

        assertThatThrownBy(() -> accessControlService.requireAllowedGrantTarget(Role.CARRIER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyAdminAndGatewayCanOperateCrossChain() {
        accessControlService.requireCrossChainOperator(user(Role.ADMIN, null));
        accessControlService.requireCrossChainOperator(user(Role.CROSS_CHAIN_GATEWAY, 1L));

        assertThatThrownBy(() -> accessControlService.requireCrossChainOperator(user(Role.CONSIGNOR, 10L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User user(Role role, Long orgId) {
        User user = new User();
        user.setRole(role);
        if (orgId != null) {
            var organization = new com.example.waybill.entity.Organization();
            organization.setId(orgId);
            user.setOrganization(organization);
        }
        return user;
    }
}
