package com.pratham.livo.service.impl;

import com.pratham.livo.config.RazorpayConfig;
import com.pratham.livo.dto.payment.PaymentInitResponseDto;
import com.pratham.livo.dto.payment.PaymentVerifyRequestDto;
import com.pratham.livo.entity.Booking;
import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Payment;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.enums.PaymentStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.BookingRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.PaymentRepository;
import com.pratham.livo.service.PaymentService;
import com.pratham.livo.utils.IdempotencyUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final static String KEY_PREFIX = "payment:idempotency:";
    private final static long PAYMENT_SESSION_TIME_LIMIT = 10L; //in minutes
    private final IdempotencyUtil idempotencyUtil;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayConfig razorpayConfig;
    private final RazorpayClient razorpayClient;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public PaymentInitResponseDto initPayment(Long bookingId, UUID idempotencyKey) {
        log.info("Initiating payment for booking with id: {}",bookingId);
        if(idempotencyKey == null){
            throw new BadRequestException("Idempotency key is missing");
        }
        //lock the idempotency key
        String redisKey = KEY_PREFIX + idempotencyKey;
        if(!idempotencyUtil.acquireLock(redisKey,PAYMENT_SESSION_TIME_LIMIT)){
            throw new BadRequestException("Payment is already initiated.");
        }

        //find booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Booking not found"));

        //ensure booking status to allow new payment or retry payment
        if(booking.getBookingStatus()!=BookingStatus.GUESTS_ADDED &&
                booking.getBookingStatus()!=BookingStatus.PAYMENT_PENDING
        ){
            throw new BadRequestException("Booking is not in a valid state.");
        }

        //check if payment row is already existing for this booking
        Payment existingPayment = paymentRepository.findByBooking(booking).orElse(null);
        if(existingPayment!=null){
            if(existingPayment.getPaymentStatus()== PaymentStatus.SUCCESSFUL){
                throw new BadRequestException("Payment already completed for this booking.");
            }
            else if(existingPayment.getPaymentStatus()==PaymentStatus.REFUNDED){
                throw new BadRequestException("Payment already refunded for this booking.");
            }
            return PaymentInitResponseDto.builder()
                    .bookingId(bookingId).amount(existingPayment.getAmount())
                    .currency("INR").razorpayKeyId(razorpayConfig.getKeyId())
                    .razorpayOrderId(existingPayment.getRazorpayOrderId())
                    .build();
        }
        try{
            //create request to create a razorpay order
            JSONObject orderRequest = new JSONObject();
            //money in paise
            orderRequest.put("amount",booking.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
            orderRequest.put("currency","INR");
            orderRequest.put("receipt","txn_"+booking.getId());

            //use razorpay client and create an order for this
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            //get order id for the created order
            String razorpayOrderId = razorpayOrder.get("id");

            //create a payment row with status = pending
            Payment payment = Payment.builder()
                    .amount(booking.getAmount())
                    .razorpayOrderId(razorpayOrderId)
                    .booking(booking)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            //save the payment
            paymentRepository.save(payment);
            //update booking status and save it
            booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
            bookingRepository.save(booking);

            //return response
            return PaymentInitResponseDto.builder()
                    .bookingId(booking.getId()).amount(booking.getAmount())
                    .currency("INR").razorpayOrderId(razorpayOrderId)
                    .razorpayKeyId(razorpayConfig.getKeyId()).build();

        } catch (RazorpayException e) {
            log.error("Razorpay Order Creation Failed", e);
            throw new RuntimeException("Failed to initiate payment with Razorpay");
        }
    }

    @Override
    @Transactional
    public boolean verifyPaymentFromClient(PaymentVerifyRequestDto paymentVerifyRequestDto) {
        log.info("Initiating payment verification for razorpayOrderId: {}",paymentVerifyRequestDto.getRazorpayOrderId());
        //get the payment record for the razorpay order id
        Payment payment = paymentRepository.findByRazorpayOrderId(paymentVerifyRequestDto.getRazorpayOrderId())
                .orElseThrow(()->new ResourceNotFoundException("Payment record not found"));

        //if already successful then return (due to webhook) -> idempotency
        if(payment.getPaymentStatus() == PaymentStatus.SUCCESSFUL){
            log.info("Payment verified for razorpayOrderId: {}",paymentVerifyRequestDto.getRazorpayOrderId());
            return true;
        }

        if(payment.getPaymentStatus() == PaymentStatus.REFUNDED){
            log.info("Payment cannot be verified for razorpayOrderId: {} as it is already refunded.",paymentVerifyRequestDto.getRazorpayOrderId());
            return false;
        }

        try{
            //create request object for razorpay to verify payment
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id",paymentVerifyRequestDto.getRazorpayOrderId());
            options.put("razorpay_payment_id",paymentVerifyRequestDto.getRazorpayPaymentId());
            options.put("razorpay_signature",paymentVerifyRequestDto.getRazorpaySignature());

            //verify the signature on the payment
            boolean isValid = Utils.verifyPaymentSignature(options,razorpayConfig.getKeySecret());

            //if signature is valid
            if(isValid){
                //update the payment and corresponding booking
                confirmPaymentSuccess(payment,paymentVerifyRequestDto.getRazorpayPaymentId(),
                        paymentVerifyRequestDto.getRazorpaySignature());
                log.info("Payment verified for razorpayOrderId: {}",paymentVerifyRequestDto.getRazorpayOrderId());
                return true;
            }else{
                //if signature is invalid mark status as failed
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("Payment verification failed for razorpayOrderId: {}",paymentVerifyRequestDto.getRazorpayOrderId());
                return false;
            }

        }catch (RazorpayException e) {
            log.error("Razorpay Signature Verification Error", e);
            throw new RuntimeException("Error verifying payment signature.");
        }

    }

    //helper method, to be used inside a method which uses @transactional
    private void confirmPaymentSuccess(Payment payment, String razorpayPaymentId, String razorpaySignature) {
        //explicitly fetch the latest booking state
        Booking booking = bookingRepository.findById(payment.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if(booking.getBookingStatus() == BookingStatus.EXPIRED){
            //TODO: handle async refund
            log.info("Late payment received for Expired Booking ID: {}", booking.getId());
            throw new BadRequestException("Booking has expired. Payment will be refunded.");
        }
        try{
            //update and save payment
            payment.setPaymentStatus(PaymentStatus.SUCCESSFUL);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setRazorpaySignature(razorpaySignature);
            paymentRepository.save(payment);

            //update and save corresponding booking
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            //get inventory list to be updated
            List<Inventory> inventoryList = inventoryRepository.findInventoriesForBookingConfirmation(
                    booking.getRoom().getId(),
                    booking.getStartDate(),
                    booking.getEndDate()
            );

            for(Inventory i : inventoryList){
                //move room from reserved to booked
                int reserved = i.getReservedCount() - booking.getRoomsCount();
                int booked = i.getBookedCount() + booking.getRoomsCount();
                i.setReservedCount(Math.max(0,reserved));
                i.setBookedCount(booked);
            }

            //save inventory list
            inventoryRepository.saveAll(inventoryList);
            log.info("Payment confirmed and Inventory updated for Booking ID: {}", booking.getId());
        }catch (ObjectOptimisticLockingFailureException e){
            //TODO: handle async refund
            log.info("Race condition detected for Booking ID: {}", booking.getId());
            throw new BadRequestException("Booking was not completed. Payment will be refunded.");
        }
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String webhookSignature){
        try {
            //verify the incoming webhook signature on the payload with the stored webhook secret
            boolean isValid = Utils.verifyWebhookSignature(payload,webhookSignature,razorpayConfig.getWebhookSecret());
            if(!isValid){
                throw new RazorpayException("signature mismatch");
            }
            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            //if event is payment captured then process payment
            if("payment.captured".equals(event)){
                JSONObject entity = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                //get razorpayOrderId and razorpayPaymentId
                String razorpayOrderId = entity.getString("order_id");
                String razorpayPaymentId = entity.getString("id");

                //find the payment row for this order id
                Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(()->new ResourceNotFoundException("Payment not found for order: " + razorpayOrderId));

                //if already refunded or successful then return early
                if(payment.getPaymentStatus() == PaymentStatus.REFUNDED || payment.getPaymentStatus() == PaymentStatus.SUCCESSFUL){
                    return;
                }
                //else confirm payment
                confirmPaymentSuccess(payment,razorpayPaymentId, "WEBHOOK_VERIFIED");
            }
        }catch (RazorpayException e) {
            log.error("Webhook Signature Validation Failed", e);
            throw new BadRequestException("Webhook Signature Validation Failed");
        }catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException("Error processing webhook");
        }
    }
}
