package com.yas.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class UserAddressServiceTest {

    UserAddressRepository repo = mock(UserAddressRepository.class);
    LocationService locationService = mock(LocationService.class);
    UserAddressService svc = new UserAddressService(repo, locationService);

    SecurityContext ctx = mock(SecurityContext.class);
    Authentication auth = mock(Authentication.class);

    @BeforeEach
    void before() {
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void after() {
        SecurityContextHolder.clearContext();
        clearInvocations(repo, locationService);
    }

    @Test
    void getUserAddressList_anonymous_throws() {
        when(auth.getName()).thenReturn("anonymousUser");
        assertThrows(AccessDeniedException.class, () -> svc.getUserAddressList());
    }

    @Test
    void getUserAddressList_returnsActiveSorted() {
        when(auth.getName()).thenReturn("u1");
        UserAddress a1 = UserAddress.builder().id(1L).userId("u1").addressId(10L).isActive(false).build();
        UserAddress a2 = UserAddress.builder().id(2L).userId("u1").addressId(20L).isActive(true).build();
        when(repo.findAllByUserId("u1")).thenReturn(List.of(a1, a2));

        AddressDetailVm d1 = AddressDetailVm.builder().id(10L).contactName("c1").phone("p1").addressLine1("l1").city("c").zipCode("z").districtId(1L).districtName("d").stateOrProvinceId(1L).stateOrProvinceName("s").countryId(1L).countryName("co").build();
        AddressDetailVm d2 = AddressDetailVm.builder().id(20L).contactName("c2").phone("p2").addressLine1("l2").city("c").zipCode("z").districtId(2L).districtName("d").stateOrProvinceId(2L).stateOrProvinceName("s").countryId(2L).countryName("co").build();

        when(locationService.getAddressesByIdList(List.of(10L, 20L))).thenReturn(List.of(d1, d2));

        var res = svc.getUserAddressList();
        assertEquals(2, res.size());
        // first should be active (a2)
        assertTrue(res.get(0).isActive());
        assertEquals(20L, res.get(0).id());
    }

    @Test
    void getAddressDefault_notFound_throws() {
        when(auth.getName()).thenReturn("u1");
        when(repo.findByUserIdAndIsActiveTrue("u1")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> svc.getAddressDefault());
    }

    @Test
    void getAddressDefault_returnsAddress() {
        when(auth.getName()).thenReturn("u1");
        UserAddress ua = new UserAddress(); ua.setAddressId(55L); ua.setUserId("u1");
        when(repo.findByUserIdAndIsActiveTrue("u1")).thenReturn(Optional.of(ua));
        AddressDetailVm d = AddressDetailVm.builder().id(55L).contactName("c").phone("p").addressLine1("a").city("c").zipCode("z").districtId(1L).districtName("d").stateOrProvinceId(1L).stateOrProvinceName("s").countryId(1L).countryName("co").build();
        when(locationService.getAddressById(55L)).thenReturn(d);
        var got = svc.getAddressDefault();
        assertEquals(55L, got.id());
    }

    @Test
    void createAddress_setsFirstActive_andReturnsVm() {
        when(auth.getName()).thenReturn("u1");
        when(repo.findAllByUserId("u1")).thenReturn(List.of());
        AddressVm av = AddressVm.builder().id(77L).contactName("c").phone("p").addressLine1("a").city("ci").zipCode("z").districtId(1L).stateOrProvinceId(1L).countryId(1L).build();
        when(locationService.createAddress(any(AddressPostVm.class))).thenReturn(av);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var res = svc.createAddress(new AddressPostVm("c","p","a","ci","z",1L,1L,1L));
        assertEquals(77L, res.addressGetVm().id());
        assertTrue(res.isActive());
    }

    @Test
    void deleteAddress_notFound_throws() {
        when(auth.getName()).thenReturn("u1");
        when(repo.findOneByUserIdAndAddressId("u1", 9L)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> svc.deleteAddress(9L));
    }

    @Test
    void deleteAddress_deletesWhenFound() {
        when(auth.getName()).thenReturn("u1");
        UserAddress ua = UserAddress.builder().id(5L).userId("u1").addressId(9L).isActive(false).build();
        when(repo.findOneByUserIdAndAddressId("u1", 9L)).thenReturn(ua);
        svc.deleteAddress(9L);
        verify(repo).delete(ua);
    }

    @Test
    void chooseDefaultAddress_updatesFlags_andSaves() {
        when(auth.getName()).thenReturn("u1");
        UserAddress ua1 = UserAddress.builder().id(1L).userId("u1").addressId(11L).isActive(false).build();
        UserAddress ua2 = UserAddress.builder().id(2L).userId("u1").addressId(22L).isActive(true).build();
        when(repo.findAllByUserId("u1")).thenReturn(List.of(ua1, ua2));
        svc.chooseDefaultAddress(11L);
        assertTrue(ua1.getIsActive());
        assertFalse(ua2.getIsActive());
        verify(repo).saveAll(anyList());
    }
}
