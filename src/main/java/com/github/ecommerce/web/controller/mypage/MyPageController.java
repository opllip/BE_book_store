package com.github.ecommerce.web.controller.mypage;

import com.github.ecommerce.service.exception.*;
import com.github.ecommerce.service.mypage.MyPageService;
import com.github.ecommerce.service.security.CustomUserDetails;
import com.github.ecommerce.web.dto.ApiResponse;
import com.github.ecommerce.web.dto.mypage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
@Slf4j
public class MyPageController {

    private final MyPageService myPageService;


    // 유저정보 가져오기
    @GetMapping("/getUserInfo")
    public ResponseEntity<UserInfoDTO> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        //토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new UserInfoDTO("로그인된 사용자만 이용하실 수 있습니다.", HttpStatus.UNAUTHORIZED.value()));
        }
        Integer userId = userDetails.getUserId();

        //유저 정보 가지고오기
        UserInfoDTO result = myPageService.getUserInfo(userId);
        return ResponseEntity.ok(result);
    }

    // 유저정보 수정
    @PutMapping("/putUserInfo/{id}")
    public ResponseEntity<ApiResponse<UserInfoDTO>> putUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id, @ModelAttribute UserInfoDTO userInfo,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        //토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "로그인된 사용자만 이용하실 수 있습니다.",null ));
        }
        Integer userId = userDetails.getUserId();

        if(!userId.equals(Integer.valueOf(id))){
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, MyPageStatus.USER_ERROR_FORBIDDEN.getMessage(),null ));
        }

        UserInfoDTO result = null;
        String previousImageUrl = null;
        String newImageUrl = null;
        String basicImageUrl = "https://project2-profile.s3.ap-northeast-2.amazonaws.com/basic.jpg";

        try{
            //1. 이미지 처리
            if (image != null) {
                newImageUrl = myPageService.uploadProfileImage(image);
                previousImageUrl = myPageService.getPreviousImageUrl(userId);
            }
            //2. 유저정보 처리(트랜잭션)
            result = myPageService.putUserInfo(userId, userInfo, newImageUrl);

            //3. 이미지 삭제처리(기존 이미지가 있으면 삭제)
            if (newImageUrl != null && !basicImageUrl.equals(previousImageUrl)) {
                myPageService.deleteProfileImage(previousImageUrl);
            }

            return ResponseEntity.ok(new ApiResponse<>(true, MyPageStatus.USER_INFO_PUT_RETURN.getMessage(), result));

        } catch (S3Exception e) {
            return ResponseEntity
                    .status( e.getStatusCode())
                    .body(new ApiResponse<>(false, e.getMessage(),null ));
        } catch (InvalidValueException e) {
            //새로 등록한 이미지 삭제처리(트랜잭션에서 실패한다면?)
            if (newImageUrl != null) {
                myPageService.deleteProfileImage(newImageUrl);
            }
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(),null ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, MyPageStatus.USER_INFO_ERROR.getMessage(),null ));
        }

    }


    //  장바구니 목록
    @GetMapping("/getCartItems")
    public ResponseEntity<ApiResponse<Page<CartDetailDTO>>> getCartItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        //토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, MyPageStatus.USER_NOT_FOUNDED.getMessage(),null ));
        }
        Integer userId = userDetails.getUserId();

        try{
            //장바구니 목록 가지고오기
            Page<CartDetailDTO> result = myPageService.getCartItems(userId, PageRequest.of(page, size));
            return ResponseEntity.ok(new ApiResponse<>(true, MyPageStatus.CART_ITEMS_RETURN.getMessage(), result));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, MyPageStatus.CART_ERROR.getMessage(), null));
        }

    }

    //장바구니 상세
    @GetMapping("/getCartItem/{id}")
    public ResponseEntity<CartListDTO> getCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id
    ) {
        // 토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new CartListDTO("로그인된 사용자만 이용하실 수 있습니다.", HttpStatus.UNAUTHORIZED.value()));
        }
        Integer userId = userDetails.getUserId();

        //장바구니 상세 가지고오기
        CartListDTO result = myPageService.getCartItem(userId, id);

        return ResponseEntity.ok(result);
    }

    // 장바구니 옵션 수정
    @PutMapping("/putCartOption")
    public ResponseEntity<DefaultDTO> putCartOption(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CartDetailDTO cartDetailDTO
    ) {
        // 토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new DefaultDTO("로그인된 사용자만 이용하실 수 있습니다.", HttpStatus.UNAUTHORIZED.value()));
        }
        Integer userId = userDetails.getUserId();

        //장바구니 옵션 수정
        DefaultDTO result = myPageService.putCartOption(userId, cartDetailDTO);

        return ResponseEntity.ok(result);
    }

    //  장바구니  삭제
    @DeleteMapping("/deleteCartItems")
    public ResponseEntity<DefaultDTO> deleteCartItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<CartDetailDTO> cartDetailDTOs
    ) {
        // 토큰에서 user 정보 가져오기
        if(userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new DefaultDTO("로그인된 사용자만 이용하실 수 있습니다.", HttpStatus.UNAUTHORIZED.value()));
        }
        Integer userId = userDetails.getUserId();

        if(cartDetailDTOs.size() == 0){
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new DefaultDTO(MyPageStatus.CART_ID_IS_NULL));
        }
        //장바구니 삭제
        DefaultDTO result = myPageService.deleteCartItems(userId, cartDetailDTOs);

        return ResponseEntity.ok(result);
    }


    //결제내역
    @GetMapping("/getPaymentList")
    public ResponseEntity<ApiResponse<Page<PaymentDetailDTO>>> getPaymentList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        // 토큰에서 user 정보 가져오기
        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, MyPageStatus.USER_INFO_ERROR.getMessage(),null ));
        }
        Integer userId = userDetails.getUserId();

        try{
            //결제내역 목록 가지고오기
            Page<PaymentDetailDTO> result = myPageService.getPaymentList(userId, PageRequest.of(page, size));
            return ResponseEntity.ok(new ApiResponse<>(true, MyPageStatus.PAYMENT_LIST_RETURN.getMessage(), result));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, MyPageStatus.CART_ERROR.getMessage(), null ));
        }
    }

    // 결제 상세정보
    @GetMapping("/getPaymentDetail/{id}")
    public ResponseEntity<PaymentListDTO> getPaymentDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id
    ) {
        // 토큰에서 user 정보 가져오기
        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new PaymentListDTO("로그인된 사용자만 이용하실 수 있습니다.", HttpStatus.UNAUTHORIZED.value()));
        }
        Integer userId = userDetails.getUserId();

        //결제내역 상세 가지고오기
        PaymentListDTO result = myPageService.getPaymentDetail(userId, id);

        return ResponseEntity.ok(result);
    }
}
