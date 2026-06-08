# Real Payment Channels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish ordinary-merchant real payment channels for WeChat Native Pay and Alipay page pay while preserving the current mock payment path.

**Architecture:** Keep `PayServiceImpl` as the payment orchestration layer. Move third-party SDK calls behind small gateway clients so unit tests can verify behavior without real merchant credentials or external network calls. Reuse the existing unified callback table and order state progression, adding raw-body handling for WeChat APIv3 notifications.

**Tech Stack:** Spring Boot 2.7, Java 8, Maven, `wechatpay-java` 0.2.17, `alipay-sdk-java` 4.40.837.ALL, JUnit 5, Mockito.

---

### Task 1: Add SDK Dependencies

**Files:**
- Modify: `gj-mall-server/pom.xml`
- Modify: `gj-mall-server/gj-mall-pay/pom.xml`

- [x] Add managed versions for `wechatpay-java` and `alipay-sdk-java`.
- [x] Add both SDKs as compile dependencies in `gj-mall-pay`.
- [x] Run `mvn -pl gj-mall-pay -am -DskipTests compile` and resolve dependency or Java 8 compatibility issues.

### Task 2: Make WeChat Native Pay Testable

**Files:**
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/WechatNativePayClient.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/WechatNativePayRequest.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/WechatNativePayResponse.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/WechatNativePaySdkClient.java`
- Modify: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/WechatPayAdapter.java`
- Test: `gj-mall-server/gj-mall-pay/src/test/java/com/gj/mall/pay/gateway/ChannelPayAdapterTest.java`

- [x] Replace the old failing expectation “SDK 尚未接入” with a failing test that expects a WeChat Native code URL when runtime config is complete.
- [x] Implement a client interface and adapter request/response DTOs.
- [x] Implement the SDK client with `RSAAutoCertificateConfig` and `NativePayService.prepay`.
- [x] Implement `WechatPayAdapter` amount conversion, description, notify URL, and pay result mapping.
- [x] Verify `ChannelPayAdapterTest` passes.

### Task 3: Finish Alipay Page Pay Through SDK

**Files:**
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/AlipayPagePayClient.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/AlipayPagePayRequest.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/AlipayPagePaySdkClient.java`
- Modify: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/gateway/AlipayAdapter.java`
- Test: `gj-mall-server/gj-mall-pay/src/test/java/com/gj/mall/pay/gateway/ChannelPayAdapterTest.java`

- [x] Add a failing test that proves Alipay uses a page-pay client and returns the generated form.
- [x] Implement the SDK client using `DefaultAlipayClient`, `AlipayTradePagePayRequest`, and `AlipayTradePagePayModel`.
- [x] Keep required config validation in the adapter.
- [x] Verify gateway tests pass.

### Task 4: Support WeChat APIv3 Callback Parsing

**Files:**
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/support/WechatPayCallbackPayload.java`
- Create: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/support/WechatPayCallbackParser.java`
- Modify: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/controller/PayController.java`
- Test: `gj-mall-server/gj-mall-pay/src/test/java/com/gj/mall/pay/support/WechatPayCallbackParserTest.java`

- [x] Add a failing parser test for mapping a decrypted WeChat transaction to existing callback fields.
- [x] Implement SDK notification parsing with raw body plus `Wechatpay-*` headers.
- [x] Make the controller pass raw body through for WeChat callbacks, while keeping existing Map-based behavior for mock and Alipay.
- [x] Verify parser tests and pay service callback tests pass.

### Task 5: Update Channel Readiness and Admin Status

**Files:**
- Modify: `gj-mall-server/gj-mall-pay/src/main/java/com/gj/mall/pay/service/impl/PayServiceImpl.java`
- Modify: `gj-mall-server/gj-mall-admin-api/src/main/java/com/gj/mall/admin/service/impl/AdminPaymentServiceImpl.java`
- Test: `gj-mall-server/gj-mall-pay/src/test/java/com/gj/mall/pay/service/impl/PayServiceImplTest.java`
- Test: `gj-mall-server/gj-mall-admin-api/src/test/java/com/gj/mall/admin/service/impl/AdminPaymentServiceImplTest.java`

- [x] Replace “等待 SDK 接入” status strings with “可用” when config is complete.
- [x] Preserve existing config-missing and HTTPS notify URL checks.
- [x] Verify targeted tests pass.

### Task 6: Verify

**Files:**
- Modify docs if configuration guidance changes.

- [x] Run `mvn -pl gj-mall-pay -am -Dtest=ChannelPayAdapterTest,PayCallbackSignatureSupportTest,PayServiceImplTest,WechatPayCallbackParserTest -DfailIfNoTests=false test`.
- [x] Run `mvn -pl gj-mall-admin-api -am -Dtest=AdminPaymentServiceImplTest -DfailIfNoTests=false test`.
- [x] Run `mvn -DskipTests package` if targeted tests pass.
- [x] Report exactly which checks passed and which external real-payment checks still require merchant credentials and HTTPS callbacks.
