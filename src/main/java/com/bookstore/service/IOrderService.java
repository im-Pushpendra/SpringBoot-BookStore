package com.bookstore.service;

import java.util.List;

import com.bookstore.dto.AddressDto;
import com.bookstore.dto.OrderDto;
import com.bookstore.entities.CartBook;
import com.bookstore.entities.OrderModel;

public interface IOrderService {
    public OrderModel placeOrder(OrderDto orderDto,String token);
	public List<CartBook> placeOrderByCart(String token,AddressDto address);
    public List<OrderModel> getAllOrders(String token);
    public OrderModel getOrderById(long orderId,String token);
    public OrderModel cancelOrderById(long orderId,String token);
}
