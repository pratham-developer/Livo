package com.pratham.livo.service.impl;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;
import com.pratham.livo.dto.message.RefundMessage;
import com.pratham.livo.dto.message.RefundUpdateMessage;
import com.pratham.livo.entity.Payment;
import com.pratham.livo.entity.Refund;
import com.pratham.livo.enums.PaymentStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.PaymentRepository;
import com.pratham.livo.repository.RefundRepository;
import com.pratham.livo.service.MessageConsumer;
import com.pratham.livo.service.PaymentService;
import com.pratham.livo.utils.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumerImpl implements MessageConsumer {

    private final TransactionalEmailsApi transactionalEmailsApi;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RefundRepository refundRepository;
    private final EmailSender emailSender;

    @Value("${livo.email.from.email}")
    private String fromEmail;

    @Value("${livo.email.from.name}")
    private String fromName;

    @Override
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailMessage emailMessage) {
        try{
            log.info("Sending email to {}", emailMessage.getTo());
            sendEmailWithBrevo(emailMessage);
        }catch (ApiException e){
            throw new RuntimeException("Retryable Brevo API Error", e);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    //for handling payment on webhook
    @Override
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @Transactional
    public void consumePaymentForWebhook(PaymentMessage paymentMessage) {
        try{
            log.info("Processing payment for razorpayOrderId: {}",paymentMessage.getRazorpayOrderId());
            String razorpayOrderId = paymentMessage.getRazorpayOrderId();
            String razorpayPaymentId = paymentMessage.getRazorpayPaymentId();
            //find the payment row for this order id
            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(()->new ResourceNotFoundException("Payment not found for order: " + razorpayOrderId));

            //if already refunded or successful then return early
            if(payment.getPaymentStatus() == PaymentStatus.REFUNDED || payment.getPaymentStatus() == PaymentStatus.SUCCESSFUL){
                return;
            }
            //else confirm payment
            paymentService.confirmPaymentSuccess(payment,razorpayPaymentId, "WEBHOOK_VERIFIED");
        }catch(BadRequestException e) {
            log.info("Payment processed but rejected (Refunded): {}", e.getMessage());
        }catch(Exception e) {
            log.error("System error processing payment webhook", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.REFUND_QUEUE)
    @Transactional
    public void consumeRefund(RefundMessage refundMessage) {
        try{
            //find payment
            Payment payment = paymentRepository.findByRazorpayOrderId(refundMessage.getRazorpayOrderId())
                    .orElseThrow(()->new ResourceNotFoundException("Payment not found with id: " + refundMessage.getRazorpayPaymentId()));
            if (payment.getRazorpayPaymentId() == null) {
                payment.setRazorpayPaymentId(refundMessage.getRazorpayPaymentId());
            }
            //initiate refund
            paymentService.initiateRefund(payment,refundMessage.getReason(),refundMessage.getPercentage());
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.REFUND_UPDATE_QUEUE)
    @Transactional
    public void consumeRefundUpdate(RefundUpdateMessage refundUpdateMessage) {
        log.info("Processing refund update for id: {}", refundUpdateMessage.getRazorpayRefundId());
        try {
            //fetch refund
            Refund refund = refundRepository.findByRazorpayRefundId(refundUpdateMessage.getRazorpayRefundId())
                    .orElseThrow(() -> new ResourceNotFoundException("Refund not found with razorpayId: " + refundUpdateMessage.getRazorpayRefundId()));

            //update status
            refund.setRefundStatus(refundUpdateMessage.getStatus());
            refundRepository.save(refund);

            //send email if status is processed
            if ("processed".equalsIgnoreCase(refundUpdateMessage.getStatus())) {
                try {
                    emailSender.sendEmail(
                            refund.getPayment().getBooking().getUser().getEmail(),
                            "Refund Processed",
                            "refund_processed",
                            Map.of(
                                    "userName", refund.getPayment().getBooking().getUser().getName(),
                                    "bookingId", refund.getPayment().getBooking().getId(),
                                    "paymentId", refund.getPayment().getId(),
                                    "hotelName", refund.getPayment().getBooking().getHotel().getName(), // <--- ADDED THIS
                                    "refundAmount", refund.getAmount()
                            )
                    );
                    log.info("Refund completion email sent for refund: {}", refund.getId());
                } catch (Exception e) {
                    log.error("Failed to send refund email (Non-fatal)", e);
                }
            }
        } catch (ResourceNotFoundException e) {
            log.error("Refund record missing for update: {}", refundUpdateMessage.getRazorpayRefundId());
        } catch (Exception e) {
            log.error("Error consuming refund update", e);
            throw new RuntimeException(e);
        }
    }

    private void sendEmailWithBrevo(EmailMessage emailMessage) throws ApiException {
        SendSmtpEmail email = new SendSmtpEmail();

        email.setSender(new SendSmtpEmailSender().email(fromEmail).name(fromName));
        email.setTo(Collections.singletonList(new SendSmtpEmailTo().email(emailMessage.getTo())));

        
        email.setSubject(emailMessage.getSubject());
        email.setHtmlContent(emailMessage.getHtmlContent());

        email.setTags(Arrays.asList("authentication", "security", "otp"));

        transactionalEmailsApi.sendTransacEmail(email);
    }
}
