package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.client.AuthServiceClient;
import com.erumpay.card.client.BillingKeyServiceClient;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.dto.CardRegisterResponse;
import com.erumpay.card.dto.client.AuthUserInfoResponse;
import com.erumpay.card.dto.client.BillingKeyDeleteRequest;
import com.erumpay.card.dto.client.BillingKeyDeleteResponse;
import com.erumpay.card.dto.client.BillingKeyIssueRequest;
import com.erumpay.card.dto.client.BillingKeyIssueResponse;
import com.erumpay.card.exception.BillingKeyIssueFailedException;
import com.erumpay.card.exception.BillingKeyIssuePendingException;
import com.erumpay.card.exception.BillingKeyIssueUnknownException;
import com.erumpay.card.exception.CardProductNotFoundException;
import com.erumpay.card.exception.CardRegistrationFailedException;
import com.erumpay.card.exception.DuplicateCardRegistrationException;
import com.erumpay.card.exception.InvalidExpiryYmException;
import com.erumpay.card.exception.InvalidUserBirthDateException;
import com.erumpay.card.exception.UserNotActiveException;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import feign.FeignException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CardRegistrationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-20T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private CardProductRepository cardProductRepository;

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private AuthServiceClient authServiceClient;

	@Mock
	private BillingKeyServiceClient billingKeyServiceClient;

	private CardRegistrationService cardRegistrationService;

	@BeforeEach
	void setUp() {
		cardRegistrationService = new CardRegistrationService(
			cardProductRepository,
			cardRegisteredRepository,
			authServiceClient,
			billingKeyServiceClient,
			transactionTemplate(),
			FIXED_CLOCK
		);
	}

	@Test
	void registerFailsWhenExpiryYmHasInvalidMonth() {
		CardRegisterRequest request = request("8000001234567890", "202613");

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(InvalidExpiryYmException.class);

		verify(cardProductRepository, never()).findByMockBin(any());
		verify(cardRegisteredRepository, never()).existsByUserIdAndCardProductIdAndStatusIn(any(), any(), any());
		verify(authServiceClient, never()).getUserInfo(any());
	}

	@Test
	void registerFailsWhenExpiryYmIsPastMonth() {
		CardRegisterRequest request = request("8000001234567890", "202504");

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(InvalidExpiryYmException.class);

		verify(cardProductRepository, never()).findByMockBin(any());
		verify(cardRegisteredRepository, never()).existsByUserIdAndCardProductIdAndStatusIn(any(), any(), any());
		verify(authServiceClient, never()).getUserInfo(any());
	}

	@Test
	void registerFailsWhenCardProductDoesNotExist() {
		CardRegisterRequest request = request("8000001234567890");
		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(CardProductNotFoundException.class);

		verify(cardRegisteredRepository, never()).existsByUserIdAndCardProductIdAndStatusIn(any(), any(), any());
		verify(authServiceClient, never()).getUserInfo(any());
	}

	@Test
	void registerFailsWhenSameCardProductAlreadyRegistered() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(true);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(DuplicateCardRegistrationException.class);

		verify(authServiceClient, never()).getUserInfo(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void duplicateCheckIncludesRegisteringStatus() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));

		CardRegisterResponse response = cardRegistrationService.register(request);

		assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
		ArgumentCaptor<Collection<CardStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
		verify(cardRegisteredRepository).existsByUserIdAndCardProductIdAndStatusIn(
			eq(1L),
			eq(10L),
			statusesCaptor.capture()
		);
		assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(
			CardStatus.REGISTERING,
			CardStatus.ACTIVE,
			CardStatus.PAUSED,
			CardStatus.EXPIRED
		);
		assertThat(statusesCaptor.getValue()).doesNotContain(CardStatus.DELETED);
	}

	@Test
	void registerCreatesRegisteringCardAndActivatesWithBillingKey() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));

		CardRegisterResponse response = cardRegistrationService.register(request);

		assertThat(response.getCardId()).isEqualTo(100L);
		assertThat(response.getCardProductId()).isEqualTo(10L);
		assertThat(response.getCardCompany()).isEqualTo("LotteCard");
		assertThat(response.getCardName()).isEqualTo("LOCA 365");
		assertThat(response.getMaskedNumber()).isEqualTo("8000-****-****-1234");
		assertThat(response.getCardAlias()).isEqualTo("생활비 카드");
		assertThat(response.getExpiryYm()).isEqualTo("202812");
		assertThat(response.getIsDefault()).isTrue();
		assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);

		verify(cardProductRepository).findByMockBin("800000");
		ArgumentCaptor<BillingKeyIssueRequest> issueRequestCaptor = ArgumentCaptor.forClass(BillingKeyIssueRequest.class);
		verify(billingKeyServiceClient).issue(issueRequestCaptor.capture());
		BillingKeyIssueRequest issueRequest = issueRequestCaptor.getValue();
		assertThat(issueRequest.payCardId()).isEqualTo(100L);
		assertThat(issueRequest.expiryDate()).isEqualTo("2812");
		assertThat(issueRequest.birthDate()).isEqualTo("900101");
	}

	@Test
	void registerActivatesCardWhenBillingKeyServiceReturnsActiveEcho() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("102", "existing-billing-key", "8000-****-****-1234"));

		CardRegisterResponse response = cardRegistrationService.register(request);

		assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
		assertThat(response.getMaskedNumber()).isEqualTo("8000-****-****-1234");
	}

	@Test
	void registerUsesSixDigitBirthDateAsIs() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));
		when(authServiceClient.getUserInfo(1L)).thenReturn(new AuthUserInfoResponse(1L, "900101", "ACTIVE"));

		cardRegistrationService.register(request);

		ArgumentCaptor<BillingKeyIssueRequest> issueRequestCaptor = ArgumentCaptor.forClass(BillingKeyIssueRequest.class);
		verify(billingKeyServiceClient).issue(issueRequestCaptor.capture());
		assertThat(issueRequestCaptor.getValue().birthDate()).isEqualTo("900101");
	}

	@Test
	void registerFailsBeforeCreatingRowWhenBirthDateIsInvalid() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);
		when(authServiceClient.getUserInfo(1L)).thenReturn(new AuthUserInfoResponse(1L, "", "ACTIVE"));

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(InvalidUserBirthDateException.class);

		verify(cardRegisteredRepository, never()).save(any());
		verify(billingKeyServiceClient, never()).issue(any());
	}

	@Test
	void registerFailsBeforeCreatingRowWhenUserIsNotActive() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);
		when(authServiceClient.getUserInfo(1L)).thenReturn(new AuthUserInfoResponse(1L, "19900101", "WITHDRAWN"));

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(UserNotActiveException.class);

		verify(cardRegisteredRepository, never()).save(any());
		verify(billingKeyServiceClient, never()).issue(any());
	}

	@Test
	void registerSoftDeletesRegisteringCardWhenBillingKeyIsPending() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("105", null, null));

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyIssuePendingException.class);

		ArgumentCaptor<CardRegistered> cardCaptor = ArgumentCaptor.forClass(CardRegistered.class);
		verify(cardRegisteredRepository, times(2)).save(cardCaptor.capture());
		assertThat(cardCaptor.getValue().getStatus()).isEqualTo(CardStatus.DELETED);
	}

	@Test
	void registerSoftDeletesRegisteringCardWhenBillingKeyIssueFailsExplicitly() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("200", null, null));

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyIssueFailedException.class);

		ArgumentCaptor<CardRegistered> cardCaptor = ArgumentCaptor.forClass(CardRegistered.class);
		verify(cardRegisteredRepository, times(2)).save(cardCaptor.capture());
		assertThat(cardCaptor.getValue().getStatus()).isEqualTo(CardStatus.DELETED);
	}

	@Test
	void registerSoftDeletesRegisteringCardWhenBillingKeyIssueSuccessCodeHasBlankBillingKey() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", " ", "8000-****-****-1234"));

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyIssueFailedException.class);

		ArgumentCaptor<CardRegistered> cardCaptor = ArgumentCaptor.forClass(CardRegistered.class);
		verify(cardRegisteredRepository, times(2)).save(cardCaptor.capture());
		assertThat(cardCaptor.getValue().getStatus()).isEqualTo(CardStatus.DELETED);
	}

	@Test
	void registerSoftDeletesRegisteringCardWhenBillingKeyServiceReturnsDefiniteHttpFailure() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));
		FeignException badRequest = mock(FeignException.class);
		when(badRequest.status()).thenReturn(400);
		when(billingKeyServiceClient.issue(any())).thenThrow(badRequest);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyIssueFailedException.class);

		ArgumentCaptor<CardRegistered> cardCaptor = ArgumentCaptor.forClass(CardRegistered.class);
		verify(cardRegisteredRepository, times(2)).save(cardCaptor.capture());
		assertThat(cardCaptor.getValue().getStatus()).isEqualTo(CardStatus.DELETED);
		verify(billingKeyServiceClient, times(1)).issue(any());
	}

	@Test
	void registerPreservesRegisteringCardWhenBillingKeyIssueResultIsUnknown() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));
		FeignException unavailable = mock(FeignException.class);
		when(unavailable.status()).thenReturn(503);
		when(billingKeyServiceClient.issue(any()))
			.thenThrow(unavailable)
			.thenThrow(unavailable);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyIssueUnknownException.class);

		ArgumentCaptor<CardRegistered> cardCaptor = ArgumentCaptor.forClass(CardRegistered.class);
		verify(cardRegisteredRepository).save(cardCaptor.capture());
		assertThat(cardCaptor.getValue().getStatus()).isEqualTo(CardStatus.REGISTERING);
		verify(billingKeyServiceClient, times(2)).issue(any());
	}

	@Test
	void registerRetriesOnceWhenBillingKeyIssueThrowsRuntimeException() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));
		when(billingKeyServiceClient.issue(any()))
			.thenThrow(new RuntimeException("temporary failure"))
			.thenReturn(issueResponse("100", "billing-key", "8000-****-****-1234"));

		CardRegisterResponse response = cardRegistrationService.register(request);

		assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
		verify(billingKeyServiceClient, times(2)).issue(any());
	}

	@Test
	void registerUnsetsCurrentDefaultWhenDefaultRequested() {
		CardRegisterRequest request = request("8000001234567890", "202812", true);
		CardProduct cardProduct = cardProduct(10L);
		CardRegistered currentDefault = CardRegistered.registering(1L, 99L, "기존 카드", "202812");
		ReflectionTestUtils.setField(currentDefault, "cardId", 90L);
		currentDefault.activate("old-billing-key", "9000-****-****-0000", true);
		stubSuccessfulRegistration(cardProduct, issueResponse("100", "billing-key", "8000-****-****-1234"));
		when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.of(currentDefault));

		CardRegisterResponse response = cardRegistrationService.register(request);

		assertThat(response.getIsDefault()).isTrue();
		assertThat(currentDefault.isDefaultCard()).isFalse();
		verify(cardRegisteredRepository).save(currentDefault);
	}

	@Test
	void registerCompensatesBillingKeyWhenActivationFails() {
		CardRegisterRequest request = request("8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);
		when(authServiceClient.getUserInfo(1L)).thenReturn(new AuthUserInfoResponse(1L, "19900101", "ACTIVE"));
		when(billingKeyServiceClient.issue(any()))
			.thenReturn(issueResponse("100", "billing-key", "8000-****-****-1234"));
		when(billingKeyServiceClient.delete(any()))
			.thenReturn(new BillingKeyDeleteResponse(100L, "billing-key", "100", "OK"));
		when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.empty());
		when(cardRegisteredRepository.save(any(CardRegistered.class))).thenAnswer(invocation -> {
			CardRegistered card = invocation.getArgument(0);
			if (card.getCardId() == null) {
				ReflectionTestUtils.setField(card, "cardId", 100L);
			}
			if (card.getStatus() == CardStatus.ACTIVE) {
				throw new RuntimeException("activation failed");
			}
			return card;
		});

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(CardRegistrationFailedException.class);

		ArgumentCaptor<BillingKeyDeleteRequest> deleteRequestCaptor =
			ArgumentCaptor.forClass(BillingKeyDeleteRequest.class);
		verify(billingKeyServiceClient).delete(deleteRequestCaptor.capture());
		assertThat(deleteRequestCaptor.getValue().payCardId()).isEqualTo(100L);
		assertThat(deleteRequestCaptor.getValue().billingKey()).isEqualTo("billing-key");
	}

	@Test
	void billingKeyClientDtosMaskSensitiveValuesInToString() {
		assertThat(new AuthUserInfoResponse(1L, "19900101", "ACTIVE").toString())
			.doesNotContain("19900101");
		assertThat(new BillingKeyIssueRequest(100L, "8000001234567890", "2812", "123", "12", "900101").toString())
			.doesNotContain("8000001234567890", "2812", "123", "12", "900101");
		assertThat(issueResponse("100", "billing-key", "8000-****-****-1234").toString())
			.doesNotContain("billing-key");
		assertThat(new BillingKeyDeleteRequest(100L, "billing-key").toString())
			.doesNotContain("billing-key");
		assertThat(new BillingKeyDeleteResponse(100L, "billing-key", "100", "OK").toString())
			.doesNotContain("billing-key");
	}

	private void stubSuccessfulRegistration(CardProduct cardProduct, BillingKeyIssueResponse issueResponse) {
		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);
		when(authServiceClient.getUserInfo(1L)).thenReturn(new AuthUserInfoResponse(1L, "19900101", "ACTIVE"));
		when(cardRegisteredRepository.save(any(CardRegistered.class))).thenAnswer(invocation -> {
			CardRegistered card = invocation.getArgument(0);
			if (card.getCardId() == null) {
				ReflectionTestUtils.setField(card, "cardId", 100L);
			}
			return card;
		});
		lenient().when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.empty());
		lenient().when(billingKeyServiceClient.issue(any())).thenReturn(issueResponse);
	}

	private CardRegisterRequest request(String cardNumber) {
		return request(cardNumber, "202812");
	}

	private CardRegisterRequest request(String cardNumber, String expiryYm) {
		return request(cardNumber, expiryYm, false);
	}

	private CardRegisterRequest request(String cardNumber, String expiryYm, Boolean isDefault) {
		return new CardRegisterRequest(
			1L,
			cardNumber,
			expiryYm,
			"123",
			"12",
			"생활비 카드",
			isDefault
		);
	}

	private CardProduct cardProduct(Long cardProductId) {
		CardProduct cardProduct = mock(CardProduct.class);
		when(cardProduct.getCardProductId()).thenReturn(cardProductId);
		lenient().when(cardProduct.getCardCompany()).thenReturn("LotteCard");
		lenient().when(cardProduct.getCardName()).thenReturn("LOCA 365");
		return cardProduct;
	}

	private BillingKeyIssueResponse issueResponse(String responseCode, String billingKey, String maskedNumber) {
		return new BillingKeyIssueResponse(100L, billingKey, maskedNumber, "LOTTE", responseCode, "OK");
	}

	private TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) {
			}

			@Override
			public void rollback(TransactionStatus status) {
			}
		});
	}
}
