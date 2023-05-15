package com.bookstore.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@ControllerAdvice
public class GlobalException {
	
	@ExceptionHandler(UserExceptions.class)
	public ErrorResponse handleUserException(UserExceptions ue) {
		return new ErrorResponse(ue.getMsg(),101);
	}

}