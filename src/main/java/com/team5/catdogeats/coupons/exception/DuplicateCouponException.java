package com.team5.catdogeats.coupons.exception;

public class DuplicateCouponException extends RuntimeException {
    public DuplicateCouponException(String message) {
        super(message);
    }
    public DuplicateCouponException(String message, Throwable cause) {
        super(message, cause);
    }
}
