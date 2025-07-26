package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SellerBrandImageServiceImpl 테스트")
class SellerBrandImageServiceImplTest {

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private ImageValidationUtil imageValidationUtil;

    @InjectMocks
    private SellerBrandImageServiceImpl sellerBrandImageService;

    private UserPrincipal userPrincipal;
    private Users user;
    private Sellers seller;
    private MultipartFile validImageFile;

    @BeforeEach
    void setUp() throws IOException {
        userPrincipal = new UserPrincipal("google", "12345");

        user = Users.builder()
                .id("user-uuid-123")
                .provider("google")
                .providerId("12345")
                .name("Test User")
                .role(Role.ROLE_SELLER)
                .userNameAttribute("sub")
                .build();

        seller = Sellers.builder()
                .userId("user-uuid-123")
                .user(user)
                .vendorName("테스트 상점")
                .vendorProfileImage("https://cdn.example.com/images/old_image.jpg")
                .businessNumber("123-45-67890")
                .build();

        // JPEG mock
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        validImageFile = spy(new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                jpegHeader
        ));
        when(validImageFile.getOriginalFilename()).thenReturn("test.jpg");
        when(validImageFile.getContentType()).thenReturn("image/jpeg");
        when(validImageFile.getSize()).thenReturn((long) jpegHeader.length);
    }

    @Nested
    @DisplayName("브랜드 이미지 업로드 테스트")
    class UploadBrandImageTest {

        @Test
        @DisplayName("성공: 새 브랜드 이미지 업로드")
        void uploadBrandImage_Success() throws IOException, ExecutionException, InterruptedException {
            String newUrl = "https://cdn.example.com/images/brand_user-uuid_new123.jpg";

            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(objectStorageService.uploadImage(anyString(), eq(validImageFile)))
                    .thenReturn(newUrl);

            Sellers updated = Sellers.builder()
                    .userId(user.getId())
                    .user(user)
                    .vendorName(seller.getVendorName())
                    .vendorProfileImage(newUrl)
                    .businessNumber(seller.getBusinessNumber())
                    .build();
            when(sellersRepository.save(any(Sellers.class))).thenReturn(updated);

            SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(user.getId());
            assertThat(result.vendorProfileImage()).isEqualTo(newUrl);

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(objectStorageService).uploadImage(anyString(), eq(validImageFile));
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("실패: 사용자 없음")
        void upload_UserNotFound() throws IOException {
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verifyNoInteractions(sellersRepository, objectStorageService);
        }
    }

    @Nested
    @DisplayName("브랜드 이미지 삭제 테스트")
    class DeleteBrandImageTest {

        @Test
        @DisplayName("성공: 브랜드 이미지 삭제")
        void deleteBrandImage_Success() {
            when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId(user.getId()))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.deleteVendorProfileImage(user.getId())).thenReturn(1);

            Sellers after = Sellers.builder()
                    .userId(user.getId())
                    .user(user)
                    .vendorName(seller.getVendorName())
                    .vendorProfileImage(null)
                    .businessNumber(seller.getBusinessNumber())
                    .build();
            when(sellersRepository.findByUserId(user.getId()))
                    .thenReturn(Optional.of(seller))
                    .thenReturn(Optional.of(after));

            SellerBrandImageResponseDTO result = sellerBrandImageService.deleteBrandImage(userPrincipal);

            assertThat(result.vendorProfileImage()).isNull();
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(sellersRepository).deleteVendorProfileImage(user.getId());
        }

        @Test
        @DisplayName("실패: 사용자 없음")
        void delete_UserNotFound() {
            when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sellerBrandImageService.deleteBrandImage(userPrincipal))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            verifyNoInteractions(sellersRepository, objectStorageService);
        }
    }

    @Nested
    @DisplayName("파일명 생성 및 키 추출 테스트")
    class FileHandlingTest {

        @Test
        @DisplayName("파일명 생성 검증")
        void generateFileName_Verify() throws IOException, ExecutionException, InterruptedException {
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");
            when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId(user.getId()))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any())).thenReturn(seller);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            when(objectStorageService.uploadImage(captor.capture(), eq(validImageFile)))
                    .thenReturn("https://cdn.example.com/images/generated.jpg");

            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);
            String fname = captor.getValue();
            assertThat(fname).matches("brand_user-uui_[a-f0-9]{32}\\.jpg");
        }
    }
}
